package cn.geokylin.magradar;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity {
    // 参数
    private SensorManager manager; //传感器管理器
    private SensorListener listener; //传感器监听器
    private int magDecimal = 4; //小数点显示位数
    private double[] threshold = new double[]{5.0, 200.0}; //获取目标的磁场变化阈值
    private float k = 2; //比例因子

    // 变量
    private float[] magVals = new float[]{0,0,0}; //磁场值
    private float[] magBase = new float[]{0,0,0}; //背景磁场值
    private double[] magDiff = new double[]{0,0,0,0}; //磁异常值
    private boolean didCali = false; //是否校正
    private int[] origin = new int[]{0,0,0,0};

    // 控件
    private TextView magXView; //X分量
    private TextView magYView; //Y分量
    private TextView magZView; //Z分量
    private TextView magFView; //总场
    private TextView targetView; //目标

    // 主函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /* 视图加载完毕 */
        // 控件
        magXView = this.findViewById(R.id.magXView);
        magYView = this.findViewById(R.id.magYView);
        magZView = this.findViewById(R.id.magZView);
        magFView = this.findViewById(R.id.magFView);
        targetView = this.findViewById(R.id.target);
        // 传感器监听器
        listener = new SensorListener();
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);

    }

    @Override
    protected void onResume() {
        //获取磁场传感器
        Sensor magneticSensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        manager.registerListener(listener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);

        super.onResume();
    }

    @Override
    protected void onPause() {
        manager.unregisterListener(listener);
        super.onPause();
    }

    // 背景场校正
    public void calibrateButton(View view){
        // 获取原点坐标
        if (!didCali) {
            origin[0] = targetView.getLeft();
            origin[1] = targetView.getWidth();
            origin[2] = targetView.getTop();
            origin[3] = targetView.getHeight();
        }

        magBase = magVals;
        didCali = true;
    }

    // 计算目标位置方法
    public int[] getLocation(double offsetX, double offsetY) {
        if (Math.abs(offsetX) < threshold[0] && Math.abs(offsetY) < threshold[0]) {
            return (new int[]{origin[0] - (int)(k * Math.ceil(offsetX)), origin[2] + (int)(k * Math.ceil(offsetY))});
        } else if (Math.abs(offsetX) > threshold[1] || Math.abs(offsetY) > threshold[1]) {
            return (new int[]{origin[0], origin[2]});
        } else {
            return (new int[]{origin[0] + (-(int)(k * Math.ceil(threshold[1] - Math.abs(offsetX))))*sign(offsetX), origin[2] + ((int)(k * Math.ceil(threshold[1] - Math.abs(offsetY))))*sign(offsetY)});
        }
    }

    // 符号函数
    public int sign(double input){
        if (input > 0){
            return 1;
        } else if (input < 0){
            return -1;
        } else {
            return 0;
        }
    }

    // 传感器监听器
    private final class SensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            //得到磁场的值
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                // 获取磁场数据
                magVals = event.values.clone();
                // 求异常磁场
                magDiff[0] = magVals[0] - magBase[0];
                magDiff[1] = magVals[1] - magBase[1];
                magDiff[2] = magVals[2] - magBase[2];
                magDiff[3] = Math.sqrt(Math.pow(magDiff[0],2) + Math.pow(magDiff[1],2) + Math.pow(magDiff[2],2));
                // 显示数据
                magXView.setText("X:" + (new BigDecimal(magDiff[0])).setScale(magDecimal, BigDecimal.ROUND_HALF_UP).toString() + " uT");
                magYView.setText("Y:" + (new BigDecimal(magDiff[1])).setScale(magDecimal, BigDecimal.ROUND_HALF_UP).toString() + " uT");
                magZView.setText("Z:" + (new BigDecimal(magDiff[2])).setScale(magDecimal, BigDecimal.ROUND_HALF_UP).toString() + " uT");
                magFView.setText("F:" + (new BigDecimal(magDiff[3])).setScale(magDecimal, BigDecimal.ROUND_HALF_UP).toString() + " uT");
                // 计算目标位置
                int[] location = getLocation(magDiff[0], magDiff[1]);
                // 显示目标位置
                if (didCali) {
                    targetView.setLeft(location[0]);
                    targetView.setRight(location[0] + origin[1]);
                    targetView.setTop(location[1]);
                    targetView.setBottom(location[1] + origin[3]);
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

}
