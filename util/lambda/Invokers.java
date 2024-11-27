package zombie.util.lambda;

import zombie.util.Pool;
import zombie.util.PooledObject;

public class Invokers {
   public static final class Params4 {
      public static final class CallbackStackItem extends StackItem implements Runnable {
         private ICallback invoker;
         private static final Pool s_pool = new Pool(CallbackStackItem::new);

         public void run() {
            this.invoker.accept(this.val1, this.val2, this.val3, this.val4);
         }

         public static CallbackStackItem alloc(Object var0, Object var1, Object var2, Object var3, ICallback var4) {
            CallbackStackItem var5 = (CallbackStackItem)s_pool.alloc();
            var5.val1 = var0;
            var5.val2 = var1;
            var5.val3 = var2;
            var5.val4 = var3;
            var5.invoker = var4;
            return var5;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.val4 = null;
            this.invoker = null;
         }
      }

      private static class StackItem extends PooledObject {
         Object val1;
         Object val2;
         Object val3;
         Object val4;
      }

      public interface ICallback {
         void accept(Object var1, Object var2, Object var3, Object var4);
      }
   }

   public static final class Params3 {
      public static final class CallbackStackItem extends StackItem implements Runnable {
         private ICallback invoker;
         private static final Pool s_pool = new Pool(CallbackStackItem::new);

         public void run() {
            this.invoker.accept(this.val1, this.val2, this.val3);
         }

         public static CallbackStackItem alloc(Object var0, Object var1, Object var2, ICallback var3) {
            CallbackStackItem var4 = (CallbackStackItem)s_pool.alloc();
            var4.val1 = var0;
            var4.val2 = var1;
            var4.val3 = var2;
            var4.invoker = var3;
            return var4;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.invoker = null;
         }
      }

      private static class StackItem extends PooledObject {
         Object val1;
         Object val2;
         Object val3;
      }

      public interface ICallback {
         void accept(Object var1, Object var2, Object var3);
      }
   }

   public static final class Params2 {
      public static final class CallbackStackItem extends StackItem implements Runnable {
         private ICallback invoker;
         private static final Pool s_pool = new Pool(CallbackStackItem::new);

         public void run() {
            this.invoker.accept(this.val1, this.val2);
         }

         public static CallbackStackItem alloc(Object var0, Object var1, ICallback var2) {
            CallbackStackItem var3 = (CallbackStackItem)s_pool.alloc();
            var3.val1 = var0;
            var3.val2 = var1;
            var3.invoker = var2;
            return var3;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.invoker = null;
         }
      }

      private static class StackItem extends PooledObject {
         Object val1;
         Object val2;
      }

      public interface ICallback {
         void accept(Object var1, Object var2);
      }
   }

   public static final class Params1 {
      public static final class CallbackStackItem extends StackItem implements Runnable {
         private ICallback invoker;
         private static final Pool s_pool = new Pool(CallbackStackItem::new);

         public void run() {
            this.invoker.accept(this.val1);
         }

         public static CallbackStackItem alloc(Object var0, ICallback var1) {
            CallbackStackItem var2 = (CallbackStackItem)s_pool.alloc();
            var2.val1 = var0;
            var2.invoker = var1;
            return var2;
         }

         public void onReleased() {
            this.val1 = null;
            this.invoker = null;
         }
      }

      private static class StackItem extends PooledObject {
         Object val1;
      }

      public interface ICallback {
         void accept(Object var1);
      }
   }
}
