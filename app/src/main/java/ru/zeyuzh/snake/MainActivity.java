package ru.zeyuzh.snake;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static int SHAKE_SPEED = 150; // 150 normal
    private final int SNAKE_INITIAL_LENGTH = 3; //normal
    private final byte FRUITS = 5;
    private final byte GROW_RATE = 2;
    private final byte LABIRINT_NUM = 2;

    int score = 0;

    SurfaceView surfaceView;
    RelativeLayout layoutSurfaceView;
    private int widthSteps;
    private int heightSteps;
    public final static String LOG_TAG = "log_snake";
    private final static String SAVED_SNAKE_X = "saved_snake_x";
    private final static String SAVED_SNAKE_Y = "saved_snake_y";
    private static final String SAVED_SCORE = "score";
    private static final String SAVED_COLLISION = "collision";

    private DrawingThread mThread;

    public static int STEP;
    public static float SEGMENT_SIZE;

    private static final int STATUS_PREPARE = 99;
    private static final int STATUS_START = 100;
    private static final int STATUS_STOP = 101;
    private static final int STATUS_RESTART = 102;
    private static final int STATUS_COLLISION = 103;
    private static final int STATUS_GET_FRUIT = 104;

    boolean running = false;
    boolean collision = false;
    byte grow = 0;

    private MenuItem menuStartStop;
    private MenuItem scoreMenuLabel;
    Handler backEvent;

    boolean bitmap[][];
    List<byte[]> mWallsLocations;
    List<Fruit> mFruitLocations;
    Snake uzh;

    SensorManager sensorManager;
    Sensor mAccel;
    float[] dXYZ;
    SensorEventListener sensorEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            Log.d(LOG_TAG, "Not first start");
            if (savedInstanceState.containsKey(SAVED_SNAKE_X) && savedInstanceState.containsKey(SAVED_SNAKE_Y)) {
                Log.d(LOG_TAG, "Get saved snake");
                uzh = new Snake(savedInstanceState.getIntArray(SAVED_SNAKE_X), savedInstanceState.getIntArray(SAVED_SNAKE_Y));
            }
            score = savedInstanceState.getInt(SAVED_SCORE, 0);
            collision = savedInstanceState.getBoolean(SAVED_COLLISION, false);
            showScore();
        }

        mFruitLocations = new ArrayList<>();
        mWallsLocations = new ArrayList<>();

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        layoutSurfaceView = (RelativeLayout) findViewById(R.id.layoutSurfaceView);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        dXYZ = new float[2];

        backEvent = new Reaction();

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                dXYZ[0] = event.values[0]; // X
                dXYZ[1] = event.values[1]; // Y
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                STEP = holder.getSurfaceFrame().centerX() / 20;
                SEGMENT_SIZE = (STEP * (float) 1.3) / 2;

                widthSteps = (int) holder.getSurfaceFrame().width() / STEP;
                heightSteps = (int) holder.getSurfaceFrame().height() / STEP;
                if (bitmap == null) {
                    bitmap = new boolean[widthSteps + 1][heightSteps + 1]; // Need that +1, +1 because width - widthSteps*STEP != 0 Same with height.
                }

                if (uzh == null) {
                    uzh = new Snake((int) (widthSteps / 2), (int) (heightSteps / 2), SNAKE_INITIAL_LENGTH);
                }

                mThread = new DrawingThread(holder);
                mThread.start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mThread.quit();
                mThread = null;
            }
        };

        surfaceView.getHolder().addCallback(callback);

    }

    private class Reaction extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_COLLISION:
                    collision = true;
                    menuStartStop.setIcon(R.drawable.ic_play_arrow_white_24dp);
                    showScore();
                    //mFruitLocations.clear();
                    break;
                case STATUS_GET_FRUIT:
                    showScore(msg.arg1);
                    break;
            }
        }
    }

    private class DrawingThread extends HandlerThread implements Handler.Callback {
        private SurfaceHolder mDrawingSurface;
        private Paint mPaint;
        private Handler mReceiver;

        public DrawingThread(SurfaceHolder holder) {
            super("DrawingThread");
            mDrawingSurface = holder;
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        private void snakeGo() {
            float gX = dXYZ[0];
            float gY = dXYZ[1];
            Segment headSegment = uzh.getSnake().get(0);
            Segment secondSegment = uzh.getSnake().get(1);
            if (Math.abs(gX) > Math.abs(gY)) {
                // left-right
                if (gX > 0) {
                    if (secondSegment.getX() == headSegment.getX() - 1) {
                        if (gY > 0) {
                            render(0, 1);   //down
                        } else {
                            render(0, -1);  //up
                        }
                    } else {
                        render(-1, 0); //left
                    }
                } else {
                    if (secondSegment.getX() == headSegment.getX() + 1) {
                        if (gY > 0) {
                            render(0, 1);   //down
                        } else {
                            render(0, -1);  //up
                        }
                    } else {
                        render(1, 0);   //right
                    }
                }
            } else {
                // up-down
                if (gY > 0) {
                    if (secondSegment.getY() == headSegment.getY() + 1) {
                        if (gX > 0) {
                            render(-1, 0); //left
                        } else {
                            render(1, 0);   //right
                        }
                    } else {
                        render(0, 1);   //down
                    }
                } else {
                    if (secondSegment.getY() == headSegment.getY() - 1) {
                        if (gX > 0) {
                            render(-1, 0); //left
                        } else {
                            render(1, 0);   //right
                        }
                    } else {
                        render(0, -1);  //up
                    }
                }
            }
        }

        private boolean render(int dx, int dy) {
            int snakeLength = uzh.getSnake().size();
            boolean result = true;

            try {
                sleep(SHAKE_SPEED);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!(dx == 0 && dy == 0)) {
                Segment oldHead = uzh.getSnake().get(0);
                Segment newHead = new Segment(oldHead.getX() + dx, oldHead.getY() + dy);
                //border checking
                if (newHead.getY() * STEP > mDrawingSurface.getSurfaceFrame().bottom ||
                        newHead.getY() * STEP < mDrawingSurface.getSurfaceFrame().top ||
                        newHead.getX() * STEP > mDrawingSurface.getSurfaceFrame().right ||
                        newHead.getX() * STEP < mDrawingSurface.getSurfaceFrame().left) {
                    Log.d(LOG_TAG, "Collision on the border");
                    result = false;
                    running = false;
                    backEvent.sendEmptyMessage(STATUS_COLLISION);
                } else {
                    uzh.getSnake().add(0, newHead);

                    Iterator iterator = mFruitLocations.iterator();
                    while (iterator.hasNext()) {
                        Fruit fruit = (Fruit) iterator.next();
                        if (fruit.equals(newHead)) {
                            Log.d(LOG_TAG, "Eat fruit in grid coordinate" +
                                    " X=" + fruit.getX() +
                                    " Y=" + fruit.getY());
                            grow += GROW_RATE;
                            iterator.remove();

                            Message m = new Message();
                            m.what = STATUS_GET_FRUIT;
                            m.arg1 = fruit.getReward();
                            backEvent.sendMessage(m);
                            //backEvent.sendEmptyMessage(STATUS_GET_FRUIT);
                        }
                        Log.d(LOG_TAG, "Fruit grid coordinate" +
                                " X=" + fruit.getX() +
                                " Y=" + fruit.getY());
                    }

                    genFruits(false);
                    if (grow == 0) {
                        //delete last segment
                        bitmap[uzh.getSnake().get(snakeLength).getX()][uzh.getSnake().get(snakeLength).getY()] = false;
                        uzh.getSnake().remove(snakeLength);
                    } else {
                        grow--;
                    }
                }

                //Coordinates shake's head
                Log.d(LOG_TAG, "Snake head grid coordinate" +
                        " X=" + newHead.getX() +
                        " Y=" + newHead.getY());

                //collision with other barriers
                if (result) {
                    if (bitmap[newHead.getX()][newHead.getY()]) {
                        Log.d(LOG_TAG, "Collision on the barriers");
                        result = false;
                        running = false;
                        backEvent.sendEmptyMessage(STATUS_COLLISION);
                    }
                }
            }

            // Render a frame
            Canvas c = mDrawingSurface.lockCanvas();
            if (c != null) {
                // Clear Canvas first
                c.drawColor(Color.WHITE);
                // Draw
                mPaint.setColor(getResources().getColor(R.color.colorSnake));
                mPaint.setStrokeWidth(2);

                //Draw labirint
                Iterator iterator = mWallsLocations.iterator();
                byte[] bytes;
                while (iterator.hasNext()) {
                    bytes = (byte[]) iterator.next();
                    c.drawRect(bytes[0] * STEP - STEP / 2, bytes[1] * STEP - STEP / 2, bytes[0] * STEP + STEP / 2, bytes[1] * STEP + STEP / 2, mPaint);
                    //bitmap[bytes[0]][bytes[1]] = true; //add block element to bitmap
                    //Log.d(LOG_TAG, "Render wall " + bytes[0] + " " + bytes[1]);
                }

                //Draw fruits
                mPaint.setColor(getResources().getColor(R.color.colorFruit));
                //Log.d(LOG_TAG, "Paint fruits");
                for (int i = 0; i < mFruitLocations.size(); i++) {
                    int x = mFruitLocations.get(i).getX() * STEP;
                    int y = mFruitLocations.get(i).getY() * STEP;
                    c.drawCircle(x, y, SEGMENT_SIZE, mPaint);
                    //Log.d(LOG_TAG, "Paint fruit " + i + " on stepPos " + x / STEP + " " + y / STEP + " on PixPos " + x + " " + y);
                }

                //Draw snake
                if (!collision) {
                    mPaint.setColor(getResources().getColor(R.color.colorSnake));
                } else {
                    mPaint.setColor(getResources().getColor(R.color.colorCollisionSnake));
                }
                //Log.d(LOG_TAG, "Render snake with " + snakeLength + " segments.");
                for (int i = 0; i < snakeLength; i++) {
                    //Draw segment
                    int x = uzh.getSnake().get(i).getX();
                    int y = uzh.getSnake().get(i).getY();
                    c.drawCircle(x * STEP, y * STEP, SEGMENT_SIZE, mPaint);
                    if (result) {
                        bitmap[x][y] = true;
                    }
                }

                // Release to be rendered to the screen
                mDrawingSurface.unlockCanvasAndPost(c);
            }
            return result;
        }

        @Override
        protected void onLooperPrepared() {
            mReceiver = new Handler(getLooper(), this);
            // Prepare to the rendering
            Log.d(LOG_TAG, "Start the rendering in onLooperPrepared");
            mReceiver.sendEmptyMessage(STATUS_PREPARE);
        }

        @Override
        public boolean quit() {
            // Clear all messages before dying
            mReceiver.removeCallbacksAndMessages(null);
            Log.d(LOG_TAG, "Quit from thread");
            //May be save data there...
            return super.quit();
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                //TODO Create a new item at the touch location.
                case STATUS_STOP:
                    running = false;
                    break;
                case STATUS_RESTART:
                    //clear prevoous snake from bitmap
                    for (int i = 0; i < uzh.getSnake().size(); i++) {
                        bitmap[uzh.getSnake().get(i).getX()][uzh.getSnake().get(i).getY()] = false;
                    }
                    uzh = new Snake((int) (widthSteps / 2), (int) (heightSteps / 2), SNAKE_INITIAL_LENGTH);
                    genFruits(true);
                    render(0, 0);
                    mReceiver.sendEmptyMessage(STATUS_START);
                    break;
                case STATUS_START:
                    // Render a frame
                    if (running) {
                        snakeGo();
                        // Post the next frame
                        mReceiver.sendEmptyMessage(STATUS_START);
                    }
                    break;
                case STATUS_PREPARE:
                    parseLabirint();
                    genFruits(false);
                    render(0, 0);
                    mReceiver.sendEmptyMessage(STATUS_START);
                    break;
            }
            return true;
        }

        private void genFruits(boolean clear) {
            Random random = new Random();
            if (clear)
                mFruitLocations.clear();
            while (mFruitLocations.size() < FRUITS) {
                int steppedX = random.nextInt(widthSteps);
                int steppedY = random.nextInt(widthSteps);
                if (!bitmap[steppedX][steppedY]) {
                    Fruit fruit = new Fruit(steppedX, steppedY);
                    mFruitLocations.add(fruit);
                    Log.d(LOG_TAG, "Fruit add " + steppedX + " " + steppedY);
                }
            }
        }

        private void parseLabirint() {
            if (LABIRINT_NUM > 0) {
                if (LABIRINT_NUM > 1) {
                    //Load Labirint from file
                    InputStream is = getApplicationContext().getResources().openRawResource(R.raw.lab2);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    byte[] bytes;
                    String line;
                    try {
                        line = reader.readLine();
                        while (line != null) {
                            String[] ab = line.split("-");
                            bytes = new byte[2];
                            bytes[0] = Byte.valueOf(ab[0]);
                            bytes[1] = Byte.valueOf(ab[1]);
                            //Log.d(LOG_TAG, "Add wall " + ab[0] + " " + ab[1]);
                            mWallsLocations.add(bytes);
                            bitmap[bytes[0]][bytes[1]] = true; //add block element to bitmap
                            line = reader.readLine();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, e.getMessage());
                    }
                }else{
                    //Programming generate Labirint
                    //TODO programming generate labirint
                }

            }
            Log.d(LOG_TAG, "Walls added " + mWallsLocations.size());
        }
    }


    private void showScore() {
        scoreMenuLabel.setTitle(getString(R.string.score_label) + score);
    }

    private void showScore(int deltagScore) {
        score += deltagScore;
        scoreMenuLabel.setTitle(getString(R.string.score_label) + score);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putIntArray(SAVED_SNAKE_X, uzh.getSnakeAsArrayX());
        outState.putIntArray(SAVED_SNAKE_Y, uzh.getSnakeAsArrayY());
        outState.putInt(SAVED_SCORE, score);
        outState.putBoolean(SAVED_COLLISION, collision);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //sensorManager.registerListener(sensorEventListener, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, mAccel, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuStartStop = menu.findItem(R.id.action_start);
        scoreMenuLabel = menu.findItem(R.id.action_score);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            //Select Settings in menu
            case (R.id.action_settings):
                return true;
            //Select Start/Stop in menu
            case (R.id.action_start): {
                running = !running;
                Message message = new Message();
                if (running) {
                    if (collision) {
                        collision = false;
                        Log.d(LOG_TAG, "Snake is restarted");
                        message.what = STATUS_RESTART;
                        score = 0;
                        showScore();
                        menuStartStop.setIcon(R.drawable.ic_stop_white_24dp);
                    } else {
                        Log.d(LOG_TAG, "Snake is running");
                        message.what = STATUS_START;
                        menuStartStop.setIcon(R.drawable.ic_stop_white_24dp);
                    }
                } else {
                    Log.d(LOG_TAG, "Snake is stopped");
                    message.what = STATUS_STOP;
                    menuStartStop.setIcon(R.drawable.ic_play_arrow_white_24dp);
                }
                mThread.handleMessage(message);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
