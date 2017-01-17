/*
 * Copyright (c) 2016 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.cheesefinder;


import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class CheeseActivity extends BaseSearchActivity {

    private Disposable mDisposable;


    /** Using a Button as a search observable..*/
//    protected void onStart(){
//        super.onStart();
//        Observable<String> searchTextObservable = createButtonClickObservable();
//
//        searchTextObservable
//
//                // First - Ensure that the next operator in chain will be run on the main thread.
//                .observeOn(AndroidSchedulers.mainThread())
//                // Add the doOnNext operator so that showProgressBar() will be called every time a new item is emitted.
//                .doOnNext(new Consumer<String>() {
//                    @Override
//                    public void accept(String s) throws Exception {
//                        showProgressBar();
//                    }
//                })
//                //First, specify that the next operator should be called on the I/O thread.
//                .observeOn(Schedulers.io())
//                // For each search query, you return a list of results.
//                .map(new Function<String, List<String>>() {
//                    @Override
//                    public List<String> apply(String result) throws Exception {
//                        return mCheeseSearchEngine.search(result);
//                    }
//                })
//                // Finally, specify that code down the chain should be executed on the main thread instead of on the I/O thread.
//                // In Android, all code that works with Views should execute on the main thread.
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<List<String>>() {
//                    @Override
//                    public void accept(List<String> results) throws Exception {
//                        //Don’t forget to call hideProgressBar() when you are just about to display a result.
//                        hideProgressBar();
//                        showResult(results);
//                    }
//                });
//
//
//    }


    /**
     * Observe Text Changes, perform search automatically when the user types some text
     */
    protected void onStart() {
        super.onStart();

        Observable<String> buttonClickStream = createButtonClickObservable();
        Observable<String> textChangeStream = createTextChangeObservable();

        //merge takes items from two or more observables and puts them into a single observable:
        Observable<String> searchTextObservable = Observable.merge(textChangeStream, buttonClickStream);


        //searchTextObservable
        mDisposable = searchTextObservable

                // First - Ensure that the next operator in chain will be run on the main thread.
                .observeOn(AndroidSchedulers.mainThread())
                // Add the doOnNext operator so that showProgressBar() will be called every time a new item is emitted.
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        showProgressBar();
                    }
                })
                //First, specify that the next operator should be called on the I/O thread.
                .observeOn(Schedulers.io())
                // For each search query, you return a list of results.
                .map(new Function<String, List<String>>() {
                    @Override
                    public List<String> apply(String result) throws Exception {
                        return mCheeseSearchEngine.search(result);
                    }
                })
                // Finally, specify that code down the chain should be executed on the main thread instead of on the I/O thread.
                // In Android, all code that works with Views should execute on the main thread.
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> results) throws Exception {
                        //Don’t forget to call hideProgressBar() when you are just about to display a result.
                        hideProgressBar();
                        showResult(results);
                    }
                });


    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!mDisposable.isDisposed()){
            mDisposable.dispose();
        }
    }

    //method that returns an observable that will emit strings.
    private Observable<String> createButtonClickObservable() {


        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {

                mSearchButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        emitter.onNext(mQueryEditText.getText().toString());
                    }
                });

                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mSearchButton.setOnClickListener(null);
                    }
                });
            }
        });
    }

    // Declare a method that will return an observable for text changes.
    private Observable<String> createTextChangeObservable() {
        {

            // Create textChangeObservable with create(), which takes an ObservableOnSubscribe.
            Observable<String> textChangeObservable = Observable.create(new ObservableOnSubscribe<String>() {
                @Override
                public void subscribe(final ObservableEmitter<String> emitter) throws Exception {

                    // When an observer makes a subscription, the first thing to do is to create a TextWatcher.
                    final TextWatcher watcher = new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

                        }

                        // You aren’t interested in beforeTextChanged() and afterTextChanged().
                        // When the user types and onTextChanged() triggers, you pass the new text value to an observer.
                        @Override
                        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                            emitter.onNext(charSequence.toString());
                        }

                        @Override
                        public void afterTextChanged(Editable editable) {

                        }
                    };
                    // Add the watcher to your TextView by calling addTextChangedListener().
                    mQueryEditText.addTextChangedListener(watcher);

                    // Don’t forget to remove your watcher. To do this, call emitter.setCancellable() and overwrite cancel() to call removeTextChangedListener()
                    emitter.setCancellable(new Cancellable() {
                        @Override
                        public void cancel() throws Exception {
                            mQueryEditText.removeTextChangedListener(watcher);
                        }
                    });
                }
            });

            // Finally, return the created observable with filter queries by length.
            return textChangeObservable
                    .filter(new Predicate<String>() {
                        @Override
                        public boolean test(String query) throws Exception {
                            return query.length() >= 2;
                        }
                    })
                    // debounce waits for a specified amount of time after each item emission for another item.
                    // If no item happens to be emitted during this wait, the last item is finally emitted:
                    .debounce(1000, TimeUnit.MILLISECONDS);

        }
    }


}


/**
 * Here’s the play-by-play of each step above:
 * 1 -Declare a method that will return an observable for text changes.
 * 2 -Create textChangeObservable with create(), which takes an ObservableOnSubscribe.
 * 3 -When an observer makes a subscription, the first thing to do is to create a TextWatcher.
 * 4 -You aren’t interested in beforeTextChanged() and afterTextChanged(). When the user types and onTextChanged() triggers, you pass the new text value to an observer.
 * 5 -Add the watcher to your TextView by calling addTextChangedListener().
 * 6 -Don’t forget to remove your watcher. To do this, call emitter.setCancellable() and overwrite cancel() to call removeTextChangedListener()
 * 7 -Finally, return the created observable.
 */
