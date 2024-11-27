package zombie.core.opengl;

public abstract class IOpenGLState {
   protected Value currentValue = this.defaultValue();
   private boolean dirty = true;

   public void set(Value var1) {
      if (this.dirty || !var1.equals(this.currentValue)) {
         this.setCurrentValue(var1);
         this.Set(var1);
      }

   }

   void setCurrentValue(Value var1) {
      this.dirty = false;
      this.currentValue.set(var1);
   }

   public void setDirty() {
      this.dirty = true;
   }

   Value getCurrentValue() {
      return this.currentValue;
   }

   abstract Value defaultValue();

   abstract void Set(Value var1);

   public interface Value {
      Value set(Value var1);
   }
}
