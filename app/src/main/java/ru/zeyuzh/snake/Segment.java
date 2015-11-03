package ru.zeyuzh.snake;

/**
 * Created by ZeyUzh on 31.10.2015.
 */
public class Segment {

    private int x;
    private int y;

    Segment(int incX, int incY) {
        this.x = incX;
        this.y = incY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
//        if (getClass() != obj.getClass() || Segment.class == obj.getClass())
//            return false;
        Segment other = (Segment) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        return true;
    }
}