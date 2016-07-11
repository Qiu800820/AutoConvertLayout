package main;
public class ConvertConfig {

    private static ConvertConfig mInstance;

    public static ConvertConfig getInstance(){
        if(mInstance == null){
            synchronized (ConvertConfig.class){
                if(mInstance == null){
                    mInstance = new ConvertConfig();
                }
            }
        }

        return mInstance;
    }

    public enum ConvertPrefix {
        NONE, MEMBER, UNDERSCORE;

        public boolean willModify() {
            return this != NONE;
        }
    }

    public enum ConvertFormat {
        PLAIN, ANDROID_ANNOTATIONS, BUTTER_KNIFE;

        public boolean requireAssignMethod() {
            return this == PLAIN;
        }
    }

    public enum Visibility {
        PRIVATE, PACKAGE_PRIVATE, PROTECTED
    }

    public ConvertPrefix prefix;
    public ConvertFormat format;
    public Visibility visibility;
    public boolean useSmartType;

    public ConvertConfig() {
        // default values

        prefix = ConvertPrefix.NONE;
        format = ConvertFormat.PLAIN;
        visibility = Visibility.PRIVATE;
        useSmartType = false;
    }
}

