package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

@TeleOp(name="Motors/Vision Tuning", group="Test")
public class MotorsVisionTuning extends LinearOpMode {

    private Limelight3A limelight;
    private DcMotor frontLeft, frontRight, backLeft, backRight;

    private double kpTurn = 0.03;
    private double kpDrive = 0.05;
    private double targetArea = 15.0;

    private int selectedSetting = 0;

    @Override
    public void runOpMode() {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");

        frontLeft.setDirection(DcMotor.Direction.REVERSE);

        limelight.setPollRateHz(100);
        limelight.start();

        while (opModeIsActive()) {
            if (gamepad1.dpad_down) { selectedSetting = (selectedSetting + 1) % 3; sleep(250); }
            if (gamepad1.dpad_up)   { selectedSetting = (selectedSetting - 1 + 3) % 3; sleep(250); }

            double increment = 0;
            if (gamepad1.dpad_right) increment = 1;
            if (gamepad1.dpad_left)  increment = -1;

            if (increment != 0) {
                if (selectedSetting == 0) kpTurn += increment * 0.005;
                if (selectedSetting == 1) kpDrive += increment * 0.005;
                if (selectedSetting == 2) targetArea += increment * 1.0;
                sleep(150);
            }

            double drive = 0, turn = 0, strafe = 0;
            LLResult result = limelight.getLatestResult();
            if (gamepad1.left_bumper && result != null && result.isValid()) {
                turn = result.getTx() * kpTurn;
                drive = (targetArea - result.getTa()) * kpDrive;
            } else if (!gamepad1.left_bumper) {
                drive = -gamepad1.left_stick_y;
                strafe = gamepad1.left_stick_x;
                turn = gamepad1.right_stick_x;
            }

            frontLeft.setPower(drive + strafe + turn);
            frontRight.setPower(drive - strafe - turn);
            backLeft.setPower(drive - strafe + turn);
            backRight.setPower(drive + strafe - turn);

            telemetry.addLine("Use D-Pad UP/DOWN to select, LEFT/RIGHT to change");
            telemetry.addData(selectedSetting == 0 ? ">> KP_TURN" : "KP_TURN", "%.3f", kpTurn);
            telemetry.addData(selectedSetting == 1 ? ">> KP_DRIVE" : "KP_DRIVE", "%.3f", kpDrive);
            telemetry.addData(selectedSetting == 2 ? ">> TARGET_AREA" : "TARGET_AREA", "%.1f", targetArea);
            telemetry.addLine("\n--- Tests ---");
            telemetry.addLine("L-Bumper: Test Vision Auto-Align");
            
            if (result != null && result.isValid()) {
                telemetry.addData("\nLimelight Seeing Target", "tx: %.1f, ta: %.1f", result.getTx(), result.getTa());
            }
            telemetry.update();
        }
    }
}
