package main.view;

import org.apache.http.util.TextUtils;

/**
 * Created by TUS on 2016/7/11.
 */
public class EditActionListener implements BaseListener{

    private String id;
    private String action;

    public EditActionListener(String action){
        this.action = action;
    }

    @Override
    public BaseListener addId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String toListenerString(String field) {
        if(TextUtils.isEmpty(field))
            return "";
        return String.format("\n\t%s.setOnEditorActionListener(new OnEditorActionListener() {\n" +
                "\t\t\n" +
                "\t\t@Override\n" +
                "\t\tpublic boolean onEditorAction(TextView v, int actionId, KeyEvent event) {\n" +
                "\t\t\tif(actionId == EditorInfo.%s || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)){\n" +
                "\t\t\t\t// TODO \n" +
                "\t\t\t\treturn true;\n" +
                "\t\t\t}\n" +
                "\t\t\t\n" +
                "\t\t\treturn false;\n" +
                "\t\t}\n" +
                "\t});\n", field, AndroidViewInfo.ON_SEARCH_LISTENER.equals(action)?"IME_ACTION_SEARCH":"IME_ACTION_SEND");
    }
}
