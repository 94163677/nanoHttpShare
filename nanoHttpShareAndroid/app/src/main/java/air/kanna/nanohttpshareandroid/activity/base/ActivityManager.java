package air.kanna.nanohttpshareandroid.activity.base;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2018/2/12.
 */

public class ActivityManager {
    private static List<Activity> actList = new ArrayList<Activity>();

    public static void addActivity(Activity act){
        if(act != null){
            actList.add(act);
        }
    }

    public static Activity removeActivity(Activity act){
        if(act != null){
            return actList.remove(act) ? act : null;
        }

        return null;
    }

    public static void closeApp(){
        for(int i=0; i<actList.size(); i++){
            actList.get(i).finish();
        }
    }
}
