package main.view;

/**
 * Created by TUS on 2016/7/11.
 */
public interface BaseListener {

    /**
     * 添加事件ID, 用于区分多个View
     * @param id View id
     * @return {@link BaseListener}
     */
    BaseListener addId(String id);

    /**
     * 生成ListenerString
     * @param field 引用对象
     * @return String
     */
    String toListenerString(String field);

}
