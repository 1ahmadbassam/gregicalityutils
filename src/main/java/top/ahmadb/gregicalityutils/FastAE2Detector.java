package top.ahmadb.gregicalityutils;

public class FastAE2Detector {
    private static final FastStackTracker TRACKER = new FastStackTracker();

    public static boolean isAE2() {
        try {
            Class<?>[] context = TRACKER.getStack();
            
            for (int i = 1; i < Math.min(10, context.length); i++) {
                String name = context[i].getName();
                if (name.startsWith("appeng.") || name.startsWith("co.neeve.nae2.") || name.startsWith("com.glodblock.github") || name.startsWith("io.github.phantamanta44.threng")) {
                    return true;
                }
            }
        } catch (Exception e) {
            
        }
        return false;
    }

    private static class FastStackTracker extends SecurityManager {
        public Class<?>[] getStack() {
            return getClassContext();
        }
    }
}