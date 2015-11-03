package ru.zeyuzh.snake;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZeyUzh on 31.10.2015.
 */
public class Snake {

    private List<Segment> body;

    Snake(int incX, int incY, int snakeLength) {
        this.body = new ArrayList<>();
        addTailSegment(incX, incY);
        for (int i = 0; i < snakeLength; i++) {
            addTailSegment(incX, body.get(i).getY());
        }
    }

    Snake(int[] allX, int[] allY) {
        if (allX.length == allY.length) {
            for (int i = 0; i < allX.length; i++) {
                addTailSegment(allX[i],allY[i]);
            }
        }
    }

    public void addTailSegment(int incX, int incY) {
        body.add(new Segment(incX, incY));
    }

    public List<Segment> getSnake() {
        return body;
    }

    public int[] getSnakeAsArrayX() {
        int[] resultX = new int[body.size()];
        for (int i = 0; i < body.size(); i++) {
            resultX[i] = body.get(i).getX();
        }
        return resultX;
    }

    public int[] getSnakeAsArrayY() {
        int[] resultY = new int[body.size()];
        for (int i = 0; i < body.size(); i++) {
            resultY[i] = body.get(i).getY();
        }
        return resultY;
    }
}
