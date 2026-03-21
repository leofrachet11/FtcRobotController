package org.firstinspires.ftc.teamcode;

import java.util.LinkedList;
import java.util.Queue;

public class SpindexerBrain {
    
    public enum BallColor { NONE, GREEN, PURPLE }
    
    private BallColor[] slots = {BallColor.NONE, BallColor.NONE, BallColor.NONE};
    private int slotAtIntake = 0;
    
    // The sequence of balls we want to shoot
    private Queue<BallColor> targetOrder = new LinkedList<>();

    // --- NEW: Preload and Override Methods ---
    public void setPreloads(BallColor s0, BallColor s1, BallColor s2) {
        slots[0] = s0;
        slots[1] = s1;
        slots[2] = s2;
    }

    public void forceSetSlot(int index, BallColor color) {
        if (index >= 0 && index < 3) {
            slots[index] = color;
        }
    }
    // ------------------------------------------

    public void recordIntake(BallColor color) {
        slots[slotAtIntake] = color;
        slotAtIntake = (slotAtIntake + 1) % 3;
    }

    public void addToTargetOrder(BallColor color) {
        targetOrder.add(color);
    }

    public BallColor getNextNeededColor() {
        return targetOrder.peek(); // Returns null if empty
    }

    public int getBestSlotToShoot(BallColor targetColor) {
        if (targetColor == null) return -1;
        for (int i = 0; i < 3; i++) {
            if (slots[i] == targetColor) return i;
        }
        return -1;
    }

    public void clearSlot(int index) {
        slots[index] = BallColor.NONE;
        if (!targetOrder.isEmpty()) targetOrder.remove();
    }

    public BallColor getSlotContent(int index) { return slots[index]; }
    public int getQueuedCount() { return targetOrder.size(); }
}
