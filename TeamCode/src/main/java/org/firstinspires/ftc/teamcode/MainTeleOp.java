package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoControllerEx;

@TeleOp(name="Main TeleOp", group="Linear Opmode")
public class MainTeleOp extends LinearOpMode {

    // Hardware
    private Limelight3A limelight;
    private DcMotor frontLeft, frontRight, backLeft, backRight;
    private DcMotor shooterWheelLeft, shooterWheelRight, intakeMotor;
    private Servo spindexerServo, armServo;

    private SpindexerBrain spindexer = new SpindexerBrain();

    // TUNED CONSTANTS (Update these after using TuningOpMode!)
    private final double KP_TURN = 0.03;
    private final double KP_DRIVE = 0.05;
    private final double TARGET_AREA = 15.0;
    private final double SHOOTER_POWER = 1.0;

    // FINAL CALIBRATED POSITIONS
    private final double[] INTAKE_POS = {0.02, 0.42, 0.82};
    private final double[] SHOOTER_POS = {0.62, 1.00, 0.22};
    private final double ARM_DOWN = 0.475;
    private final double ARM_UP = 1.0;

    private int currentSlotIndex = 0;
    
    // Intake toggle state
    private boolean isIntakeOn = false;
    private boolean lastRightBumper = false;

    @Override
    public void runOpMode() {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");
        
        shooterWheelLeft = hardwareMap.get(DcMotor.class, "shooterWheelLeft");
        shooterWheelRight = hardwareMap.get(DcMotor.class, "shooterWheelRight");

        intakeMotor = hardwareMap.get(DcMotor.class, "intakeMotor");
        
        spindexerServo = hardwareMap.get(Servo.class, "spindexerServo");
        armServo = hardwareMap.get(Servo.class, "armServo");

        setServoRange(spindexerServo);
        setServoRange(armServo);

        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        shooterWheelRight.setDirection(DcMotor.Direction.REVERSE);
        
        // Intake is reversed so positive power pulls balls IN
        intakeMotor.setDirection(DcMotor.Direction.REVERSE);

        // PRELOADS: Setup the brain to assume 3 balls are loaded. Color doesn't matter anymore.
        spindexer.setPreloads(
            SpindexerBrain.BallColor.PURPLE,
            SpindexerBrain.BallColor.PURPLE,
            SpindexerBrain.BallColor.PURPLE
        );
        
        currentSlotIndex = 0;
        spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);

        limelight.start();
        armServo.setPosition(ARM_DOWN); 

        telemetry.addLine("Initialized.");
        telemetry.addLine("PRELOADS SET: [FULL] [FULL] [FULL]");
        telemetry.addData("Limelight Connected", limelight.isConnected());
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // --- 1. DRIVER CONTROLS ---
            double drive = -gamepad1.left_stick_y;
            double strafe = gamepad1.left_stick_x;
            double turn = gamepad1.right_stick_x;

            // --- 2. LIMELIGHT AUTO-ALIGN (Left Bumper) ---
            // Assuming your Limelight is now configured by default to track AprilTags
            LLResult result = limelight.getLatestResult();
            if (gamepad1.left_bumper && result != null && result.isValid()) {
                turn = result.getTx() * KP_TURN;
                drive = -gamepad1.left_stick_y; // Keep manual forward/back drive
            }

            frontLeft.setPower(drive + strafe + turn);
            frontRight.setPower(drive - strafe - turn);
            backLeft.setPower(drive - strafe + turn);
            backRight.setPower(drive + strafe - turn);

            // --- 3. INTAKE MOTOR LOGIC (TOGGLE) ---
            boolean currentRightBumper = gamepad1.right_bumper;
            if (currentRightBumper && !lastRightBumper) {
                isIntakeOn = !isIntakeOn; // Toggle state
            }
            lastRightBumper = currentRightBumper;

            if (gamepad1.right_trigger > 0.1) {
                intakeMotor.setPower(-1.0); // Reverse to un-jam or spit out
            } else if (isIntakeOn) {
                intakeMotor.setPower(1.0);  // Runs if toggled ON
            } else {
                intakeMotor.setPower(0.0);  // Off otherwise
            }

            // --- 4. RECORD INTAKE (Button A) ---
            if (gamepad1.a) {
                // Log a generic ball and move to next slot
                spindexer.recordIntake(SpindexerBrain.BallColor.GREEN);
                currentSlotIndex = (currentSlotIndex + 1) % 3;
                spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);
                sleep(300); // Debounce
            }

            // --- 5. SHOOT ONE (Button B) ---
            if (gamepad1.b) {
                int slot = spindexer.getAnyFilledSlot();
                if (slot != -1) {
                    shooterWheelLeft.setPower(SHOOTER_POWER);
                    shooterWheelRight.setPower(SHOOTER_POWER);
                    
                    spindexerServo.setPosition(SHOOTER_POS[slot]);
                    sleep(800); 

                    armServo.setPosition(ARM_UP);
                    sleep(600); 
                    armServo.setPosition(ARM_DOWN);
                    sleep(400); 

                    shooterWheelLeft.setPower(0);
                    shooterWheelRight.setPower(0);
                    
                    spindexer.clearSlot(slot);
                    spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);
                }
            }

            // --- 6. SHOOT ALL RAPID-FIRE (Button X) ---
            if (gamepad1.x) {
                // Spin up wheels ONCE for all balls
                shooterWheelLeft.setPower(SHOOTER_POWER);
                shooterWheelRight.setPower(SHOOTER_POWER);
                sleep(500); // Wait for spin-up

                // Loop through every slot and shoot if it has a ball
                for (int i = 0; i < 3; i++) {
                    if (spindexer.getSlotContent(i) != SpindexerBrain.BallColor.NONE) {
                        spindexerServo.setPosition(SHOOTER_POS[i]);
                        sleep(600); // Wait for spindexer to arrive

                        armServo.setPosition(ARM_UP);
                        sleep(600);
                        armServo.setPosition(ARM_DOWN);
                        sleep(400);

                        spindexer.clearSlot(i);
                    }
                }

                // Spin down wheels and return home
                shooterWheelLeft.setPower(0);
                shooterWheelRight.setPower(0);
                spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);
            }
            
            // --- TELEMETRY ---
            telemetry.addLine("\n--- Spindexer Status ---");
            telemetry.addData("Slot 0", spindexer.getSlotContent(0) != SpindexerBrain.BallColor.NONE ? "FULL" : "EMPTY");
            telemetry.addData("Slot 1", spindexer.getSlotContent(1) != SpindexerBrain.BallColor.NONE ? "FULL" : "EMPTY");
            telemetry.addData("Slot 2", spindexer.getSlotContent(2) != SpindexerBrain.BallColor.NONE ? "FULL" : "EMPTY");
            telemetry.addData("Current Intake Slot", currentSlotIndex);
            
            telemetry.addLine("\n--- RAPID FIRE CONTROLS ---");
            telemetry.addLine("Drive: L/R Sticks");
//            telemetry.addLine("Hold L-Bumper: AprilTag Auto-Align");
            telemetry.addLine("R-Bumper: TOGGLE Intake On/Off");
            telemetry.addLine("Hold R-Trigger: REVERSE Intake to Unjam");
            telemetry.addLine("A Button: Log Pickup");
            telemetry.addLine("B Button: Shoot 1 Ball");
            telemetry.addLine("X Button: SHOOT ALL BALLS (RAPID FIRE)");
            
            telemetry.update();
        }
    }

    private void setServoRange(Servo s) {
        if (s.getController() instanceof ServoControllerEx) {
            ((ServoControllerEx) s.getController()).setServoPwmRange(s.getPortNumber(), new PwmControl.PwmRange(500, 2500));
        }
    }
}
