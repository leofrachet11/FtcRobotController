package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import java.util.List;

@TeleOp(name="Test: Auto-Align 3D Distance", group="Test")
public class DistanceAutoAlignTester extends LinearOpMode {

    private Limelight3A limelight;
    private DcMotor frontLeft, frontRight, backLeft, backRight;

    private double kpTurn = 0.03;
    private double kpDrive = 0.8;
    private double kpStrafe = 0.02;

    private double targetDistance = 1.0; 
    
    private boolean lastDpadUp = false;
    private boolean lastDpadDown = false;

    @Override
    public void runOpMode() {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");

        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.FORWARD);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.FORWARD);

        limelight.setPollRateHz(100);
        limelight.start();

        telemetry.addLine("Ready to test 3D Distance Auto-Align.");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            
            // --- ADJUST TARGET DISTANCE (Meters) ---
            if (gamepad1.dpad_up && !lastDpadUp) targetDistance += 0.05;
            if (gamepad1.dpad_down && !lastDpadDown) targetDistance -= 0.05;
            
            lastDpadUp = gamepad1.dpad_up;
            lastDpadDown = gamepad1.dpad_down;

            // --- DRIVE LOGIC ---
            double drive = -gamepad1.left_stick_y;
            double strafe = gamepad1.left_stick_x;
            double turn = gamepad1.right_stick_x;

            LLResult result = limelight.getLatestResult();
            
            if (gamepad1.left_bumper && result != null && result.isValid()) {
                List<LLResultTypes.FiducialResult> tags = result.getFiducialResults();
                if (tags != null && !tags.isEmpty()) {
                    LLResultTypes.FiducialResult tag = tags.get(0);
                    Pose3D pose = tag.getTargetPoseCameraSpace();
                    
                    // 3D Distance to tag in meters
                    double currentDistance = Math.sqrt(
                        Math.pow(pose.getPosition().x, 2) + 
                        Math.pow(pose.getPosition().y, 2)
                    );
                    
                    // Yaw angle (to square up to the board)
                    double yaw = pose.getOrientation().getYaw(AngleUnit.DEGREES);

                    // We want to turn to center the tag (tx -> 0)
                    // We want to drive to the target distance
                    // We want to strafe to square up to the tag (yaw -> 0)
                    
                    turn = result.getTx() * kpTurn; 
                    drive = (currentDistance - targetDistance) * kpDrive; 
                    strafe = yaw * kpStrafe; 
                } else {
                    // Fallback to basic turn if no 3D tag data
                    turn = result.getTx() * kpTurn;
                }
            }

            frontLeft.setPower(drive + strafe + turn);
            frontRight.setPower(drive - strafe - turn);
            backLeft.setPower(drive - strafe + turn);
            backRight.setPower(drive + strafe - turn);

            // --- TELEMETRY ---
            telemetry.addLine("--- 3D DISTANCE & SQUARING TUNING ---");
            telemetry.addLine("Hold Left Bumper to Auto-Align, Drive & Square Up!");
            telemetry.addLine("Use D-Pad UP/DOWN to adjust Target Distance (Meters)");
            telemetry.addData("\nYour Target Distance", "%.2f meters", targetDistance);
            
            if (result != null && result.isValid()) {
                List<LLResultTypes.FiducialResult> tags = result.getFiducialResults();
                if (tags != null && !tags.isEmpty()) {
                    Pose3D pose = tags.get(0).getTargetPoseCameraSpace();
                    double dist = Math.sqrt(Math.pow(pose.getPosition().x, 2) + Math.pow(pose.getPosition().y, 2));
                    double yaw = pose.getOrientation().getYaw(AngleUnit.DEGREES);
                    telemetry.addData("Tag Found", "Dist: %.2fm | Yaw: %.1f° | tx: %.1f", dist, yaw, result.getTx());
                    telemetry.addData("Distance Error", "%.2fm (Drive Pwr: %.2f)", dist - targetDistance, (dist - targetDistance) * kpDrive);
                } else {
                    telemetry.addData("Target Found (No 3D)", "tx: %.1f, ta: %.2f", result.getTx(), result.getTa());
                }
            } else {
                telemetry.addLine("\nNO TARGET FOUND");
            }
            
            telemetry.update();
        }
    }
}
