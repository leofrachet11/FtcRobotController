package org.firstinspires.ftc.teamcode;

import java.util.LinkedList;
import java.util.Queue;

public class SpindexerBrain {
    
    // We keep the enum but treat GREEN as a generic "BALL"
    public enum BallColor { NONE, GREEN, PURPLE }
    
    private BallColor[] slots = {BallColor.NONE, BallColor.NONE, BallColor.NONE};
    private int slotAtIntake = 0;
    
    private Queue<BallColor> targetOrder = new LinkedList<>();

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

    public void recordIntake(BallColor color) {
        slots[slotAtIntake] = color;
        slotAtIntake = (slotAtIntake + 1) % 3;
    }

    // --- NEW: SPEED STRATEGY ---
    // Finds ANY slot that has a ball in it, regardless of color.
    public int getAnyFilledSlot() {
        for (int i = 0; i < 3; i++) {
            if (slots[i] != BallColor.NONE) return i;
        }
        return -1; // Completely empty
    }

    public void clearSlot(int index) {
        slots[index] = BallColor.NONE;
        if (!targetOrder.isEmpty()) targetOrder.remove();
    }

    public BallColor getSlotContent(int index) { return slots[index]; }
}
