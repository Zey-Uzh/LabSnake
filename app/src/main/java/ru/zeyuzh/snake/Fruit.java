package ru.zeyuzh.snake;

import java.util.Random;

/**
 * Created by ZeyUzh on 03.11.2015.
 */
public class Fruit extends Segment {
    private short reward;

    public Fruit(int x, int y){
        super(x,y);
        Random r = new Random();
        int tmp = r.nextInt(3);
        switch (tmp){
            case 0:
                reward = 75;
                break;
            case 1:
                reward = 100;
                break;
            case 2:
                reward = 125;
                break;
        }
    }

    public int getReward() {
        return reward;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
