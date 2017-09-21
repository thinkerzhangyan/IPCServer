package tk.thinkerzhangyan.ipcserver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyService extends Service {

    private CopyOnWriteArrayList<Book> bookList = new CopyOnWriteArrayList();

    private RemoteCallbackList<IOnNewBookArrivedListener> listenerList = new RemoteCallbackList<>();

    private AtomicBoolean mIsServiceDestoryed = new AtomicBoolean(false);

    Binder mBinder = new IBookManager.Stub(){

        @Override
        public List<Book> getBookList() throws RemoteException {
            return bookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            bookList.add(book);
            Log.d("TAG", "book:"+book.bookId+":"+book.bookName);
        }

        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
            listenerList.register(listener);
        }

        @Override
        public void unregisterListenr(IOnNewBookArrivedListener listener) throws RemoteException {
                listenerList.unregister(listener);

        }
    };


    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bookList.add(new Book(0,"Android第一行代码"));
        bookList.add(new Book(1,"Android群英传"));
        Log.d("TAG","onCreate");

        new Thread(new ServiceWorker()).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsServiceDestoryed.set(true);
    }

    private class ServiceWorker implements Runnable {
        @Override
        public void run() {
            // do background processing here.....
            while (!mIsServiceDestoryed.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int bookId = bookList.size() + 1;
                Book newBook = new Book(bookId, "new book#" + bookId);
                try {
                    onNewBookArrived(newBook);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onNewBookArrived(Book book) throws RemoteException {

        bookList.add(book);
        final int N = listenerList.beginBroadcast();
        for (int i = 0; i < N; i++) {
            IOnNewBookArrivedListener l = listenerList.getBroadcastItem(i);
            if (l != null) {
                try {
                    l.onNewBookArrived(book);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        listenerList.finishBroadcast();

    }
}
