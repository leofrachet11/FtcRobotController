package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

@TeleOp(name="Motors and Vision Tuning", group="Test")
public class TuningOpMode extends LinearOpMode {

    private Limelight3A limelight;
    private DcMotor frontLeft, frontRight, backLeft, backRight, shooterLeft, shooterRight;

    // Tuning Variables
    private double kpTurn = 0.03;
    private double kpDrive = 0.05;
    private double targetArea = 15.0;
    private double shooterPower = 1.0;

    // UI State
    private int selectedSetting = 0; // 0=kpTurn, 1=kpDrive, 2=targetArea, 3=shooterPower

    @Override
    public void runOpMode() {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");
        shooterLeft = hardwareMap.get(DcMotor.class, "shooterWheelLeft");
        shooterRight = hardwareMap.get(DcMotor.class, "shooterWheelRight");

        // Use corrected directions
        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.FORWARD);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.FORWARD);
        
        shooterLeft.setDirection(DcMotor.Direction.FORWARD);
        shooterRight.setDirection(DcMotor.Direction.REVERSE);

        limelight.setPollRateHz(100);
        limelight.start();

        telemetry.addLine("Ready to Tune.");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            // --- UI CONTROLS ---
            if (gamepad1.dpad_down) { selectedSetting = (selectedSetting + 1) % 4; sleep(250); }
            if (gamepad1.dpad_up)   { selectedSetting = (selectedSetting - 1 + 4) % 4; sleep(250); }

            double increment = 0;
            if (gamepad1.dpad_right) increment = 1;
            if (gamepad1.dpad_left)  increment = -1;

            if (increment != 0) {
                if (selectedSetting == 0) kpTurn += increment * 0.005;
                if (selectedSetting == 1) kpDrive += increment * 0.005;
                if (selectedSetting == 2) targetArea += increment * 1.0;
                if (selectedSetting == 3) shooterPower += increment * 0.05;
                sleep(150);
            }
            shooterPower = Math.max(0, Math.min(1.0, shooterPower)); // Cap power

            // --- TEST AUTO-ALIGN (Hold Left Bumper) ---
            double drive = 0, turn = 0, strafe = 0;
            LLResult result = limelight.getLatestResult();
            if (gamepad1.left_bumper && result != null && result.isValid()) {
                turn = result.getTx() * kpTurn;
                drive = (targetArea - result.getTa()) * kpDrive;
            } else if (!gamepad1.left_bumper) { // Manual drive
                drive = -gamepad1.left_stick_y;
                strafe = gamepad1.left_stick_x;
                turn = gamepad1.right_stick_x;
            }

            frontLeft.setPower(drive + strafe + turn);
            frontRight.setPower(drive - strafe - turn);
            backLeft.setPower(drive - strafe + turn);
            backRight.setPower(drive + strafe - turn);

            // --- TEST SHOOTER (Hold Right Trigger) ---
            if (gamepad1.right_trigger > 0.1) {
                shooterLeft.setPower(shooterPower);
                shooterRight.setPower(shooterPower);
            } else {
                shooterLeft.setPower(0);
                shooterRight.setPower(0);
            }

            // --- TELEMETRY ---
            telemetry.addLine("Use D-Pad UP/DOWN to select, LEFT/RIGHT to change");
            telemetry.addData(selectedSetting == 0 ? ">> KP_TURN" : "KP_TURN", "%.3f", kpTurn);
            telemetry.addData(selectedSetting == 1 ? ">> KP_DRIVE" : "KP_DRIVE", "%.3f", kpDrive);
            telemetry.addData(selectedSetting == 2 ? ">> TARGET_AREA" : "TARGET_AREA", "%.1f", targetArea);
            telemetry.addData(selectedSetting == 3 ? ">> SHOOTER_POWER" : "SHOOTER_POWER", "%.2f", shooterPower);
            telemetry.addLine("\n--- Tests ---");
            telemetry.addLine("Hold Left Bumper: Test Vision Auto-Align");
            telemetry.addLine("Hold Right Trigger: Test Shooter Wheel Power");
            
            if (result != null && result.isValid()) {
                telemetry.addData("\nLimelight Seeing Target", "tx: %.1f, ta: %.1f", result.getTx(), result.getTa());
            }
            telemetry.update();
        }
    }
}
