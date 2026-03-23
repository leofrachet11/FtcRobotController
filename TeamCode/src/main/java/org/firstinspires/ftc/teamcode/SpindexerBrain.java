package org.firstinspires.ftc.teamcode;

import java.util.ArrayList;
import java.util.List;

public class SpindexerBrain {

    private boolean[] slots = {false, false, false};

    public void preloadBalls(boolean slot0, boolean slot1, boolean slot2) {
        slots[0] = slot0;
        slots[1] = slot1;
        slots[2] = slot2;
    }

    public void logBall(int index) {
        slots[index] = true;
    }

    public void clearBall(int index) {
        slots[index] = false;
    }

    public boolean isBall(int index) {
        return slots[index]; 
    }

    public List<Integer> getShootableSlotsOrder(int currentIntakeSlot) {
        List<Integer> shootOrder = new ArrayList<>();
        
        int prevSlot = (currentIntakeSlot + 2) % 3;
        int currSlot = currentIntakeSlot;
        int nextSlot = (currentIntakeSlot + 1) % 3;

        if (slots[prevSlot]) shootOrder.add(prevSlot);
        if (slots[currSlot]) shootOrder.add(currSlot);
        if (slots[nextSlot]) shootOrder.add(nextSlot);

        return shootOrder;
    }

    public int getClosestShootableSlot(int currentIntakeSlot) {
        List<Integer> order = getShootableSlotsOrder(currentIntakeSlot);
        if (order.isEmpty()) {
            return -1;
        }
        return order.get(0);
    }
}
