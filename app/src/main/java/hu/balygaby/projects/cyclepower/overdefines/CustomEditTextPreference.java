package hu.balygaby.projects.cyclepower.overdefines;

import android.content.Context;
import android.util.AttributeSet;

public class CustomEditTextPreference extends android.preference.EditTextPreference {
 
    public CustomEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
     
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
         
        setSummary(getSummary());
    }
 
    @Override
    public CharSequence getSummary() {
        return this.getText();
    }

   /* public void setError(CharSequence error){
        if (getEditText().getText()==null) TextView.setError("error");
    }*/
}