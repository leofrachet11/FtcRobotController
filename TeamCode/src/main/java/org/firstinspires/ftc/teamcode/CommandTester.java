package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoControllerEx;

@TeleOp(name="Command Tester", group="Test")
public class CommandTester extends LinearOpMode {

    private DcMotor shooterWheelLeft, shooterWheelRight;
    private Servo spindexerServo, armServo;

    // FINAL CALIBRATED POSITIONS
    private final double[] INTAKE_POS = {0.02, 0.42, 0.82};
    private final double[] SHOOTER_POS = {0.62, 1.00, 0.22};
    private final double ARM_DOWN = 0.475;
    private final double ARM_UP = 1.0;

    private int slotIndex = 0;
    private boolean intakeSide = true;

    @Override
    public void runOpMode() {
        shooterWheelLeft = hardwareMap.get(DcMotor.class, "shooterWheelLeft");
        shooterWheelRight = hardwareMap.get(DcMotor.class, "shooterWheelRight");
        spindexerServo = hardwareMap.get(Servo.class, "spindexerServo");
        armServo = hardwareMap.get(Servo.class, "armServo");

        setServoRange(spindexerServo);
        setServoRange(armServo);

        shooterWheelRight.setDirection(DcMotor.Direction.REVERSE);
        armServo.setPosition(ARM_DOWN);
        spindexerServo.setPosition(INTAKE_POS[slotIndex]);

        telemetry.addLine("Ready to test. Press Start.");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // 1. Cycle Slots (A Button)
            if (gamepad1.a) {
                slotIndex = (slotIndex + 1) % 3;
                double pos = intakeSide ? INTAKE_POS[slotIndex] : SHOOTER_POS[slotIndex];
                spindexerServo.setPosition(pos);
                sleep(300);
            }

            // 2. Toggle Side (B Button)
            if (gamepad1.b) {
                intakeSide = !intakeSide;
                double pos = intakeSide ? INTAKE_POS[slotIndex] : SHOOTER_POS[slotIndex];
                spindexerServo.setPosition(pos);
                sleep(300);
            }

            // 3. Test Full Shoot Sequence (X Button)
            if (gamepad1.x) {
                shooterWheelLeft.setPower(1.0);
                shooterWheelRight.setPower(1.0);
                
                // Move current slot to shooter side
                spindexerServo.setPosition(SHOOTER_POS[slotIndex]);
                sleep(800);
                
                // Fire arm
                armServo.setPosition(ARM_UP);
                sleep(600);
                armServo.setPosition(ARM_DOWN);
                sleep(400);
                
                shooterWheelLeft.setPower(0);
                shooterWheelRight.setPower(0);
            }

            // 4. Individual Arm Test (Y Button)
            if (gamepad1.y) {
                armServo.setPosition(ARM_UP);
                sleep(600);
                armServo.setPosition(ARM_DOWN);
            }

            telemetry.addData("Slot Index", slotIndex);
            telemetry.addData("Side", intakeSide ? "INTAKE" : "SHOOTER");
            telemetry.addLine("\n--- Controls ---");
            telemetry.addLine("A: Cycle Slots");
            telemetry.addLine("B: Toggle Intake/Shooter Side");
            telemetry.addLine("X: Run FULL Shoot Cycle");
            telemetry.addLine("Y: Test Arm Only");
            telemetry.update();
        }
    }

    private void setServoRange(Servo s) {
        if (s.getController() instanceof ServoControllerEx) {
            ((ServoControllerEx) s.getController()).setServoPwmRange(s.getPortNumber(), new PwmControl.PwmRange(500, 2500));
        }
    }
}
