package eu.faircode.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FairEmail is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018-2019 by Marcel Bokhorst (M66B)
*/

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.preference.PreferenceManager;
import androidx.room.paging.LimitOffsetDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class ViewModelMessages extends ViewModel {
    private AdapterMessage.ViewType last = AdapterMessage.ViewType.UNIFIED;
    private Map<AdapterMessage.ViewType, Model> models = new HashMap<>();

    private ExecutorService executor = Helper.getBackgroundExecutor(2, "model");

    private static final int LOCAL_PAGE_SIZE = 100;
    private static final int REMOTE_PAGE_SIZE = 10;
    private static final int SEARCH_PAGE_SIZE = 1;
    private static final int LOW_MEM_MB = 32;

    Model getModel(
            final Context context, final LifecycleOwner owner,
            final AdapterMessage.ViewType viewType,
            String type, long account, long folder,
            String thread, long id,
            String query, boolean server) {

        Args args = new Args(context, viewType, type, account, folder, thread, id, query, server);
        Log.i("Get model=" + viewType + " " + args);
        dump();

        Model model = models.get(viewType);
        if (model == null || !model.args.equals(args)) {
            Log.i("Creating model=" + viewType + " replace=" + (model != null));

            if (model != null)
                model.list.removeObservers(owner);

            DB db = DB.getInstance(context);

            BoundaryCallbackMessages boundary = null;
            if (viewType == AdapterMessage.ViewType.FOLDER)
                boundary = new BoundaryCallbackMessages(context,
                        args.folder, true, args.query, REMOTE_PAGE_SIZE);
            else if (viewType == AdapterMessage.ViewType.SEARCH)
                boundary = new BoundaryCallbackMessages(context,
                        args.folder, args.server, args.query,
                        args.server ? REMOTE_PAGE_SIZE : SEARCH_PAGE_SIZE);

            LivePagedListBuilder<Integer, TupleMessageEx> builder = null;
            switch (viewType) {
                case UNIFIED:
                    builder = new LivePagedListBuilder<>(
                            db.message().pagedUnified(
                                    args.type,
                                    args.threading,
                                    args.sort, args.ascending,
                                    args.filter_seen, args.filter_unflagged, args.filter_snoozed,
                                    false,
                                    args.debug),
                            LOCAL_PAGE_SIZE);
                    break;

                case FOLDER:
                    PagedList.Config configFolder = new PagedList.Config.Builder()
                            .setInitialLoadSizeHint(LOCAL_PAGE_SIZE)
                            .setPageSize(LOCAL_PAGE_SIZE)
                            .setPrefetchDistance(REMOTE_PAGE_SIZE)
                            .build();
                    builder = new LivePagedListBuilder<>(
                            db.message().pagedFolder(
                                    args.folder, args.threading,
                                    args.sort, args.ascending,
                                    args.filter_seen, args.filter_unflagged, args.filter_snoozed,
                                    false,
                                    args.debug),
                            configFolder);
                    builder.setBoundaryCallback(boundary);
                    break;

                case THREAD:
                    builder = new LivePagedListBuilder<>(
                            db.message().pagedThread(
                                    args.account, args.thread,
                                    args.threading ? null : args.id,
                                    args.ascending,
                                    args.debug), LOCAL_PAGE_SIZE);
                    break;

                case SEARCH:
                    PagedList.Config configSearch = new PagedList.Config.Builder()
                            .setPageSize(LOCAL_PAGE_SIZE)
                            .setPrefetchDistance(REMOTE_PAGE_SIZE)
                            .build();
                    if (args.folder < 0)
                        builder = new LivePagedListBuilder<>(
                                db.message().pagedUnified(
                                        null,
                                        args.threading,
                                        "time", false,
                                        false, false, false,
                                        true,
                                        args.debug),
                                configSearch);
                    else
                        builder = new LivePagedListBuilder<>(
                                db.message().pagedFolder(
                                        args.folder, args.threading,
                                        "time", false,
                                        false, false, false,
                                        true,
                                        args.debug),
                                configSearch);
                    builder.setBoundaryCallback(boundary);
                    break;
            }

            builder.setFetchExecutor(executor);

            model = new Model(args, builder.build(), boundary);
            models.put(viewType, model);
        }

        owner.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void onDestroyed() {
                int free_mb = Log.getFreeMemMb();
                boolean lowmem = (free_mb < LOW_MEM_MB);

                Log.i("Destroy model=" + viewType +
                        " lowmem=" + lowmem + " free=" + free_mb + " MB");

                Model model = models.get(viewType);
                if (model != null) {
                    Log.i("Remove observer model=" + viewType);
                    model.list.removeObservers(owner);
                }

                if (viewType == AdapterMessage.ViewType.THREAD || lowmem) {
                    Log.i("Remove model=" + viewType);
                    models.remove(viewType);
                }

                dump();
            }
        });

        if (viewType == AdapterMessage.ViewType.UNIFIED) {
            models.remove(AdapterMessage.ViewType.FOLDER);
            models.remove(AdapterMessage.ViewType.SEARCH);
        } else if (viewType == AdapterMessage.ViewType.FOLDER)
            models.remove(AdapterMessage.ViewType.SEARCH);

        if (viewType != AdapterMessage.ViewType.THREAD) {
            last = viewType;
            Log.i("Last model=" + last);
        }

        Log.i("Returning model=" + viewType);
        dump();

        return model;
    }

    @Override
    protected void onCleared() {
        for (AdapterMessage.ViewType viewType : new ArrayList<>(models.keySet()))
            models.remove(viewType);
    }

    void observePrevNext(LifecycleOwner owner, final long id, final IPrevNext intf) {
        Log.i("Observe prev/next model=" + last);

        Model model = models.get(last);
        if (model == null) {
            Log.w("Observe previous/next without model");
            return;
        }

        Log.i("Observe previous/next id=" + id);
        model.list.observe(owner, new Observer<PagedList<TupleMessageEx>>() {
            @Override
            public void onChanged(PagedList<TupleMessageEx> messages) {
                Log.i("Observe previous/next id=" + id + " messages=" + messages.size());

                for (int pos = 0; pos < messages.size(); pos++) {
                    TupleMessageEx item = messages.get(pos);
                    if (item != null && id == item.id) {
                        boolean load = false;

                        if (pos - 1 >= 0) {
                            TupleMessageEx next = messages.get(pos - 1);
                            if (next == null)
                                load = true;
                            intf.onNext(true, next == null ? null : next.id);
                        } else
                            intf.onNext(false, null);

                        if (pos + 1 < messages.size()) {
                            TupleMessageEx prev = messages.get(pos + 1);
                            if (prev == null)
                                load = true;
                            intf.onPrevious(true, prev == null ? null : prev.id);
                        } else
                            intf.onPrevious(false, null);

                        intf.onFound(pos, messages.size());

                        if (load)
                            messages.loadAround(pos);

                        return;
                    }
                }

                Log.w("Observe previous/next gone id=" + id);
            }
        });
    }

    void getIds(Context context, LifecycleOwner owner, final Observer<List<Long>> observer) {
        final Model model = models.get(last);
        if (model == null) {
            Log.w("Get IDs without model");
            observer.onChanged(new ArrayList<Long>());
            return;
        }

        new SimpleTask<List<Long>>() {
            @Override
            protected List<Long> onExecute(Context context, Bundle args) {
                List<Long> ids = new ArrayList<>();

                PagedList<TupleMessageEx> plist = model.list.getValue();
                if (plist == null)
                    return ids;

                LimitOffsetDataSource<TupleMessageEx> ds = (LimitOffsetDataSource<TupleMessageEx>) plist.getDataSource();
                int count = ds.countItems();
                for (int i = 0; i < count; i += 100)
                    for (TupleMessageEx message : ds.loadRange(i, Math.min(100, count - i)))
                        if (message.uid != null)
                            ids.add(message.id);

                Log.i("Loaded messages #" + ids.size());
                return ids;
            }

            @Override
            protected void onExecuted(Bundle args, List<Long> ids) {
                observer.onChanged(ids);
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                observer.onChanged(new ArrayList<Long>());
            }
        }.execute(context, owner, new Bundle(), "model:ids");
    }

    private class Args {
        private long account;
        private String type;
        private long folder;
        private String thread;
        private long id;
        private String query;
        private boolean server;

        private boolean threading;
        private String sort;
        private boolean ascending;
        private boolean filter_seen;
        private boolean filter_unflagged;
        private boolean filter_snoozed;
        private boolean debug;

        Args(Context context,
             AdapterMessage.ViewType viewType,
             String type, long account, long folder,
             String thread, long id,
             String query, boolean server) {

            this.type = type;
            this.account = account;
            this.folder = folder;
            this.thread = thread;
            this.id = id;
            this.query = query;
            this.server = server;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            this.threading = prefs.getBoolean("threading", true);
            this.sort = prefs.getString("sort", "time");
            this.ascending = prefs.getBoolean(
                    viewType == AdapterMessage.ViewType.THREAD ? "ascending_thread" : "ascending_list", false);
            this.filter_seen = prefs.getBoolean("filter_seen", false);
            this.filter_unflagged = prefs.getBoolean("filter_unflagged", false);
            this.filter_snoozed = prefs.getBoolean("filter_snoozed", true);
            this.debug = prefs.getBoolean("debug", false);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof Args) {
                Args other = (Args) obj;
                return (Objects.equals(this.type, other.type) &&
                        this.account == other.account &&
                        this.folder == other.folder &&
                        Objects.equals(this.thread, other.thread) &&
                        this.id == other.id &&
                        Objects.equals(this.query, other.query) &&
                        this.server == other.server &&

                        this.threading == other.threading &&
                        Objects.equals(this.sort, other.sort) &&
                        this.ascending == other.ascending &&
                        this.filter_seen == other.filter_seen &&
                        this.filter_unflagged == other.filter_unflagged &&
                        this.filter_snoozed == other.filter_snoozed &&
                        this.debug == other.debug);
            } else
                return false;
        }

        @NonNull
        @Override
        public String toString() {
            return "folder=" + type + ":" + account + ":" + folder +
                    " thread=" + thread + ":" + id +
                    " query=" + query + ":" + server + "" +
                    " threading=" + threading +
                    " sort=" + sort + ":" + ascending +
                    " filter seen=" + filter_seen + " unflagged=" + filter_unflagged + " snoozed=" + filter_snoozed +
                    " debug=" + debug;
        }
    }

    private void dump() {
        Log.i("Current models=" + TextUtils.join(", ", models.keySet()));
    }

    class Model {
        private Args args;
        private LiveData<PagedList<TupleMessageEx>> list;
        private BoundaryCallbackMessages boundary;

        Model(Args args, LiveData<PagedList<TupleMessageEx>> list, BoundaryCallbackMessages boundary) {
            this.args = args;
            this.list = list;
            this.boundary = boundary;
        }

        void setCallback(LifecycleOwner owner, BoundaryCallbackMessages.IBoundaryCallbackMessages callback) {
            if (boundary != null) {
                boundary.setCallback(callback);

                owner.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    public void onDestroyed() {
                        boundary.close();
                    }
                });
            }
        }

        void setObserver(LifecycleOwner owner, @NonNull Observer<PagedList<TupleMessageEx>> observer) {
            //list.removeObservers(owner);
            list.observe(owner, observer);
        }
    }

    interface IPrevNext {
        void onPrevious(boolean exists, Long id);

        void onNext(boolean exists, Long id);

        void onFound(int position, int size);
    }
}
