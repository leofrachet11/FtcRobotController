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
    private int pipeline = 0; // 0 = Green, 1 = Purple, 2 = AprilTag

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
        armServo = hardwareMap.get(Servo.class, "armLeft"); 

        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.FORWARD);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.FORWARD);

        shooterWheelLeft.setDirection(DcMotor.Direction.FORWARD);
        shooterWheelRight.setDirection(DcMotor.Direction.REVERSE);
        
        // Intake is reversed so positive power pulls balls IN
        intakeMotor.setDirection(DcMotor.Direction.REVERSE);

        // PRELOADS: Setup the brain to assume Purple, Purple, Green
        spindexer.setPreloads(
            SpindexerBrain.BallColor.PURPLE,
            SpindexerBrain.BallColor.PURPLE,
            SpindexerBrain.BallColor.GREEN
        );
        
        // Ensure starting slot is 0, since it is already full, 
        // we will shoot first to empty it, or manually override it.
        currentSlotIndex = 0;

        limelight.start();
        armServo.setPosition(ARM_DOWN); 

        telemetry.addLine("Initialized.");
        telemetry.addLine("PRELOADS SET: [PURPLE] [PURPLE] [GREEN]");
        telemetry.addData("Limelight Connected", limelight.isConnected());
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // --- 1. PIPELINE SWITCHING (B Button) ---
            if (gamepad1.b) {
                pipeline = (pipeline + 1) % 3;
                limelight.pipelineSwitch(pipeline);
                sleep(250);
            }

            // --- 2. DRIVER CONTROLS ---
            double drive = -gamepad1.left_stick_y;
            double strafe = gamepad1.left_stick_x;
            double turn = gamepad1.right_stick_x;

            // --- 3. LIMELIGHT AUTO-ALIGN (Left Bumper) ---
            LLResult result = limelight.getLatestResult();
            if (gamepad1.left_bumper && result != null && result.isValid()) {
                turn = result.getTx() * KP_TURN;
                
                if (pipeline == 2) {
                    drive = -gamepad1.left_stick_y; 
                } else {
                    drive = (TARGET_AREA - result.getTa()) * KP_DRIVE;
                }
            }

            frontLeft.setPower(drive + strafe + turn);
            frontRight.setPower(drive - strafe - turn);
            backLeft.setPower(drive - strafe + turn);
            backRight.setPower(drive + strafe - turn);

            // --- 4. INTAKE MOTOR LOGIC (ALWAYS ON) ---
            // Reverses on Right Trigger, stops on Right Bumper, otherwise always runs IN.
            if (gamepad1.right_trigger > 0.1) {
                intakeMotor.setPower(-1.0); // Reverse to spit out
            } else if (gamepad1.right_bumper) {
                intakeMotor.setPower(0); // Optional: Panic stop
            } else {
                intakeMotor.setPower(1.0); // ALWAYS ON default
            }

            // --- 5. RECORD INTAKE (D-Pad) ---
            if (gamepad1.dpad_up || gamepad1.dpad_down) {
                SpindexerBrain.BallColor color = gamepad1.dpad_up ? SpindexerBrain.BallColor.GREEN : SpindexerBrain.BallColor.PURPLE;
                spindexer.recordIntake(color);
                
                currentSlotIndex = (currentSlotIndex + 1) % 3;
                spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);
                sleep(300);
            }

            // --- 6. SHOOT LOGIC (A=Green, X=Purple) ---
            if (gamepad1.a || gamepad1.x) {
                SpindexerBrain.BallColor target = gamepad1.a ? SpindexerBrain.BallColor.GREEN : SpindexerBrain.BallColor.PURPLE;
                int slot = spindexer.getBestSlotToShoot(target);

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
            
            // --- 7. CO-PILOT OVERRIDES (GAMEPAD 2) ---
            // If the brain gets confused, the second controller can manually force slot colors
            if (gamepad2.a) {
                spindexer.forceSetSlot(currentSlotIndex, SpindexerBrain.BallColor.GREEN);
                sleep(200);
            } else if (gamepad2.x) {
                spindexer.forceSetSlot(currentSlotIndex, SpindexerBrain.BallColor.PURPLE);
                sleep(200);
            } else if (gamepad2.b) {
                spindexer.forceSetSlot(currentSlotIndex, SpindexerBrain.BallColor.NONE); // Clear slot
                sleep(200);
            }

            // Manually rotate spindexer to next or previous slot (without recording intake)
            if (gamepad2.dpad_right) {
                currentSlotIndex = (currentSlotIndex + 1) % 3;
                spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);
                sleep(250);
            } else if (gamepad2.dpad_left) {
                currentSlotIndex = (currentSlotIndex + 2) % 3; // +2 modulo 3 is equivalent to -1
                spindexerServo.setPosition(INTAKE_POS[currentSlotIndex]);
                sleep(250);
            }
            
            // --- TELEMETRY ---
            String pipeName = (pipeline == 0) ? "GREEN" : (pipeline == 1) ? "PURPLE" : "APRILTAG";
            
            telemetry.addData("Limelight", limelight.isConnected() ? "ONLINE" : "OFFLINE");
            telemetry.addData("Pipeline", "%d: %s", pipeline, pipeName);
            
            telemetry.addLine("\n--- Spindexer Status ---");
            telemetry.addData("Slot 0", spindexer.getSlotContent(0));
            telemetry.addData("Slot 1", spindexer.getSlotContent(1));
            telemetry.addData("Slot 2", spindexer.getSlotContent(2));
            telemetry.addData("Active Slot (Gamepad 2 Controls)", currentSlotIndex);
            
            telemetry.addLine("\n--- Driver Controls ---");
            telemetry.addLine("Hold L-Bumper: Vision Auto-Align");
            telemetry.addLine("Hold R-Trigger: REVERSE Intake | R-Bumper: STOP Intake");
            telemetry.addLine("D-Pad UP/DOWN: Log Green/Purple");
            telemetry.addLine("A/X Button: Shoot Green/Purple");

            telemetry.addLine("\n--- Gamepad 2 Overrides ---");
            telemetry.addLine("A: Force Active Slot to GREEN");
            telemetry.addLine("X: Force Active Slot to PURPLE");
            telemetry.addLine("B: Force Active Slot EMPTY");
            telemetry.addLine("D-Pad L/R: Manually rotate Spindexer");
            
            telemetry.update();
        }
    }
}
