package main.view;

import org.apache.http.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TUS on 2016/7/11.
 */
public class OnClick implements BaseListener{

    private List<String> ids;

    private static OnClick mInstance;

    public static OnClick getInstance(){
        if(mInstance == null){
            synchronized (OnClick.class){
                if(mInstance == null)
                    mInstance = new OnClick();
            }
        }

        return mInstance;
    }


    @Override
    public BaseListener addId(String id) {
        if(TextUtils.isEmpty(id))
            return this;

        if(ids == null){
            ids = new ArrayList<String>();
        }

        ids.add(id);
        return this;
    }

    @Override
    public String toListenerString(String field) {
        if(ids != null && ids.size() > 0){
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\nView.OnClickListener listener = new View.OnClickListener() {\n\t\n" +
                    "\t@Override\n" +
                    "\tpublic void onClick(View v) {\n" +
                    "\t\tswitch (v.getId()) {\n");

            for(String id : ids){
                stringBuilder.append(String.format("\t\t\tcase %s:\n\t\t\t\tbreak;\n", id));
            }

            stringBuilder.append("\t\t\tdefault:\n" +
                    "\t\t\t\tbreak;\n" +
                    "\t\t}\n" +
                    "\t}\n" +
                    "};");
            return stringBuilder.toString();
        }

        return "";
    }


}
