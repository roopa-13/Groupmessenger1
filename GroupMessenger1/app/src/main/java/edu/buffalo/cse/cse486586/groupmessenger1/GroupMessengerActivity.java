package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    private final String TAG = GroupMessengerActivity.class.getName();

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;

    List<String> portList = Arrays.asList(REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4);

    private static int sequenceNo=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.d(TAG,"PORT : " + myPort.toString());

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new GroupMessengerActivity.ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
        }


        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText editText = (EditText) findViewById(R.id.editText1);
                final String msg = editText.getText().toString(); // + "\n";
                editText.setText("");

                TextView textView = (TextView) findViewById(R.id.textView1);
                textView.append("\n");

                new GroupMessengerActivity.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Log.d(TAG, "ServerTask - doInBackground() Started");
            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             * Below modified code is based on understanding Oracle (2019), documentation (source code)
             * - https://docs.oracle.com/javase/tutorial/networking/sockets/
             */

            while (true) {
                try {
                    //Accepting the incoming connection request from the client
                    Socket clientSocket = serverSocket.accept();
                    DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                    String msg = in.readUTF();
                    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                    Log.i(TAG, "ServerTask - doInBackground() Client Connection established with port: " + clientSocket.getPort());

                    publishProgress(msg);
                    out.writeUTF("ACK \n");
                    Log.i(TAG, "ServerTask - ACK sent to the client " + clientSocket.getInetAddress() + clientSocket.getPort());

                    in.close();
                    out.flush();
                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "ServerTask socket IOException ");
                }
            }
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();

            Log.d(TAG, "ServerTask - onProgressUpdate() Started");

            Log.i(TAG, "ServerTask - onProgressUpdate() Message Update "+strReceived);

            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            insertToDb(strReceived);

            return;
        }

    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {
        //Socket socket;
        @Override
        protected Void doInBackground(String... msgs) {
                try {

                    String msgToSend = msgs[0].trim();
                    /*
                     * TODO: Fill in your client code that sends out a message.
                     * Below modified code is based on understanding Oracle (2019), documentation (source code)
                     * - https://docs.oracle.com/javase/tutorial/networking/sockets/
                     */

                    if (null != msgToSend || !msgToSend.isEmpty()) {
                        for (String port : portList) {
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port));
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                            out.writeUTF(msgToSend);

                            Log.i(TAG, "ClientTask - doInBackground() - Message sent");

                            DataInputStream in = new DataInputStream(socket.getInputStream());
                            String ackString = in.readUTF();
                            if (ackString.contains("ACK")) {
                                in.close();
                                out.flush();
                                out.close();
                                socket.close();
                                Log.i(TAG, "ClientTask - doInBackground() - ACK received fromm server : "+ socket.getInetAddress()+
                                        socket.getPort());
                            }
                        }
                    }

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException:" + e.toString());
                }
                return null;
            }

    }

    public void insertToDb(String input){
        Log.d(TAG,"ServerTask - onProgressUpdate() - insertToDb() started");

        ContentValues keyValueToInsert = new ContentValues();

        keyValueToInsert.put(GroupMessengerDbHelper.KEY_FIELD, Integer.toString(sequenceNo));
        keyValueToInsert.put(GroupMessengerDbHelper.VALUE_FIELD, input);

        sequenceNo++;
        getContentResolver().insert(GroupMessengerProvider.BASE_URI, keyValueToInsert);

        Log.d(TAG, "ServerTask - onProgressUpdate() - insertToDb() message inserted");
    }
}


