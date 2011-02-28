package org.i18nchecker.test1;

/**
 *
 * @author Petr
 */
public class TestA {

    public static final String X = "should be localized";
    
    public TestA() {
        int x = 1;
        assert x == 1 : "String which are written in the same line as 'assert' keyword are ignored";
        
        System.out.println("Also all strings with (trim().lenght() <= 1) are ignored like this one:"); // NOI18N
        System.out.println("x");
        
        System.out.println(NbBundle.getMessage(TestA.class, "Key_correct"));
        System.out.println(NbBundle.getMessage(TestA.class, "Key_missing"));
        System.out.println("Intentionally no-i18n string"); // NOI18N
        
        String PREFIX = "NAME_"; // NOI18N
        System.out.println("Concatenate resource bundle string:"); // NOI18N
        System.out.println(PREFIX + "key"); // NOI18N
    }
}
