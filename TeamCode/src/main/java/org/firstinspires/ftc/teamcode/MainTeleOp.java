package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoControllerEx;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import java.util.List;

@TeleOp(name="Main TeleOp", group="Linear Opmode")
public class MainTeleOp extends LinearOpMode {

    private Limelight3A limelight;
    private DcMotor frontLeft, frontRight, backLeft, backRight;
    private DcMotorEx shooterWheelLeft, shooterWheelRight;
    private DcMotor intakeMotor;
    private Servo spindexerServo, armServo;

    private SpindexerBrain spindexer = new SpindexerBrain();
    private int currentSlotIndex = 0;

    private final double KP_TURN = 0.03;
    private final double KP_DRIVE = 0.8;
    private final double KP_STRAFE = 0.02;
    private double targetDistance = 1.0;
    private final double PRECISION_SENSITIVITY = 0.3;
    private final double SHOOTER_TARGET_VELOCITY = 1450;

    private final double[] INTAKE_POS = {0.02, 0.42, 0.82};
    private final double[] SHOOTER_POS = {0.62, 1.00, 0.22};
    private final double ARM_DOWN = 0.475;
    private final double ARM_UP = 1.0;

    private boolean isIntakeOn = false;
    private boolean lastRightBumper = false;
    
    private boolean lastDpadUp = false;
    private boolean lastDpadDown = false;
    private boolean lastYButton = false;

    @Override
    public void runOpMode() {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");

        shooterWheelLeft = hardwareMap.get(DcMotorEx.class, "shooterWheelLeft");
        shooterWheelRight = hardwareMap.get(DcMotorEx.class, "shooterWheelRight");

        intakeMotor = hardwareMap.get(DcMotor.class, "intakeMotor");
        
        spindexerServo = hardwareMap.get(Servo.class, "spindexerServo");
        armServo = hardwareMap.get(Servo.class, "armServo");

        setServoRange(spindexerServo);
        setServoRange(armServo);

        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        shooterWheelRight.setDirection(DcMotor.Direction.REVERSE);
        intakeMotor.setDirection(DcMotor.Direction.REVERSE);

        shooterWheelLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooterWheelRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        spindexer.preloadBalls(true, true, true);
        
        currentSlotIndex = 0;
        spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);

        limelight.start();
        armServo.setPosition(ARM_DOWN);

        waitForStart();

        while (opModeIsActive()) {
            if (gamepad1.dpad_up && !lastDpadUp) targetDistance += 0.05;
            if (gamepad1.dpad_down && !lastDpadDown) targetDistance -= 0.05;
            lastDpadUp = gamepad1.dpad_up;
            lastDpadDown = gamepad1.dpad_down;

            updateDriveAndIntake(false);

            if (gamepad1.a) {
                spindexer.logBall(currentSlotIndex);
                currentSlotIndex = (currentSlotIndex + 1) % 3;
                spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);
                activeSleep(300);
            }

            boolean currentY = gamepad1.y;
            if (currentY && !lastYButton) {
                currentSlotIndex = (currentSlotIndex + 2) % 3; 
                spindexer.clearBall(currentSlotIndex);
                spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);
                activeSleep(300);
            }
            lastYButton = currentY;

            if (gamepad1.b) {
                int slot = spindexer.getClosestShootableSlot(currentSlotIndex);
                if (slot != -1) {
                    shooterWheelLeft.setVelocity(SHOOTER_TARGET_VELOCITY);
                    shooterWheelRight.setVelocity(SHOOTER_TARGET_VELOCITY);
                    
                    spindexerServo.setPosition(SHOOTER_POS[slot]);
                    activeSleep(800); 

                    armServo.setPosition(ARM_UP);
                    activeSleep(600); 
                    armServo.setPosition(ARM_DOWN);
                    activeSleep(400);

                    shooterWheelLeft.setVelocity(0);
                    shooterWheelRight.setVelocity(0);
                    
                    spindexer.clearBall(slot);
                    spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);
                }
            }

            if (gamepad1.x) {
                List<Integer> slotsToShoot = spindexer.getShootableSlotsOrder(currentSlotIndex);
                
                if (!slotsToShoot.isEmpty()) {
                    shooterWheelLeft.setVelocity(SHOOTER_TARGET_VELOCITY);
                    shooterWheelRight.setVelocity(SHOOTER_TARGET_VELOCITY);
                    activeSleep(500);

                    for (int slot : slotsToShoot) {
                        spindexerServo.setPosition(SHOOTER_POS[slot]);
                        activeSleep(600); 

                        armServo.setPosition(ARM_UP);
                        activeSleep(600);
                        armServo.setPosition(ARM_DOWN);
                        activeSleep(400);

                        spindexer.clearBall(slot);
                    }

                    shooterWheelLeft.setVelocity(0);
                    shooterWheelRight.setVelocity(0);

                    spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);
                }
            }

            telemetry.addLine("\n--- Spindexer Status ---");
            telemetry.addData("Slot 0", spindexer.isBall(0) ? "FULL" : "EMPTY");
            telemetry.addData("Slot 1", spindexer.isBall(1) ? "FULL" : "EMPTY");
            telemetry.addData("Slot 2", spindexer.isBall(2) ? "FULL" : "EMPTY");
            telemetry.addData("Current Intake Slot", currentSlotIndex);

            telemetry.addLine("\n--- CONTROLS ---");
            telemetry.addLine("Drive: L/R Sticks | Hold L-Trigger: Precision Drive");
            telemetry.addLine("X: Log Ball | Y: Undo Last Pickup");
            telemetry.addLine("O: Shoot 1 Ball | □: Shoot ALL Balls");
            telemetry.addLine("R-Bumper: TOGGLE Intake On/Off | Hold R-Trigger: REVERSE");
            telemetry.addLine("Hold L-Bumper: 3D Auto-Align");
            telemetry.addData("Target Distance", "%.2f Meters (Use D-Pad Up/Down to adjust)", targetDistance);

            telemetry.update();
        }
    }

    private void activeSleep(long milliseconds) {
        long endTime = System.currentTimeMillis() + milliseconds;
        while (opModeIsActive() && System.currentTimeMillis() < endTime) {
            updateDriveAndIntake(true);
        }
    }

    private void updateDriveAndIntake(boolean isShooting) {
        double drive = -gamepad1.left_stick_y;
        double strafe = gamepad1.left_stick_x;
        double turn = gamepad1.right_stick_x;

        LLResult result = limelight.getLatestResult();
        if (gamepad1.left_bumper && result != null && result.isValid()) {
            List<LLResultTypes.FiducialResult> tags = result.getFiducialResults();
            if (tags != null && !tags.isEmpty()) {
                Pose3D pose = tags.get(0).getTargetPoseCameraSpace();

                double currentDistance = Math.sqrt(
                    Math.pow(pose.getPosition().x, 2) + 
                    Math.pow(pose.getPosition().y, 2)
                );

                double yaw = pose.getOrientation().getYaw(AngleUnit.DEGREES);

                strafe = yaw * KP_STRAFE;
                turn = result.getTx() * KP_TURN;
                drive = (currentDistance - targetDistance) * KP_DRIVE; 
            } else {
                turn = result.getTx() * KP_TURN;
            }
        }

        boolean isPrecision = gamepad1.left_trigger > 0.1 || isShooting;
        double multiplier = isPrecision ? PRECISION_SENSITIVITY : 1.0;

        frontLeft.setPower((drive + strafe + turn) * multiplier);
        frontRight.setPower((drive - strafe - turn) * multiplier);
        backLeft.setPower((drive - strafe + turn) * multiplier);
        backRight.setPower((drive + strafe - turn) * multiplier);

        boolean currentRightBumper = gamepad1.right_bumper;
        if (currentRightBumper && !lastRightBumper) {
            isIntakeOn = !isIntakeOn;
        }
        lastRightBumper = currentRightBumper;

        if (gamepad1.right_trigger > 0.1) {
            intakeMotor.setPower(-1.0);
        } else if (isIntakeOn) {
            intakeMotor.setPower(1.0);
        } else {
            intakeMotor.setPower(0.0);
        }
    }

    private void setServoRange(Servo s) {
        if (s.getController() instanceof ServoControllerEx) {
            ((ServoControllerEx) s.getController()).setServoPwmRange(s.getPortNumber(), new PwmControl.PwmRange(500, 2500));
        }
    }
}
