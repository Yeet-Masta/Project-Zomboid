package zombie.util.lambda;

import java.util.function.Predicate;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class Predicates {
   public static final class Params3 {
      public static final class CallbackStackItem extends StackItem implements Predicate {
         private ICallback predicate;
         private static final Pool s_pool = new Pool(CallbackStackItem::new);

         public boolean test(Object var1) {
            return this.predicate.test(var1, this.val1, this.val2, this.val3);
         }

         public static CallbackStackItem alloc(Object var0, Object var1, Object var2, ICallback var3) {
            CallbackStackItem var4 = (CallbackStackItem)s_pool.alloc();
            var4.val1 = var0;
            var4.val2 = var1;
            var4.val3 = var2;
            var4.predicate = var3;
            return var4;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.val3 = null;
            this.predicate = null;
         }
      }

      private static class StackItem extends PooledObject {
         Object val1;
         Object val2;
         Object val3;
      }

      public interface ICallback {
         boolean test(Object var1, Object var2, Object var3, Object var4);
      }
   }

   public static final class Params2 {
      public static final class CallbackStackItem extends StackItem implements Predicate {
         private ICallback predicate;
         private static final Pool s_pool = new Pool(CallbackStackItem::new);

         public boolean test(Object var1) {
            return this.predicate.test(var1, this.val1, this.val2);
         }

         public static CallbackStackItem alloc(Object var0, Object var1, ICallback var2) {
            CallbackStackItem var3 = (CallbackStackItem)s_pool.alloc();
            var3.val1 = var0;
            var3.val2 = var1;
            var3.predicate = var2;
            return var3;
         }

         public void onReleased() {
            this.val1 = null;
            this.val2 = null;
            this.predicate = null;
         }
      }

      private static class StackItem extends PooledObject {
         Object val1;
         Object val2;
      }

      public interface ICallback {
         boolean test(Object var1, Object var2, Object var3);
      }
   }

   public static final class Params1 {
      public static final class CallbackStackItem extends StackItem implements Predicate {
         private ICallback predicate;
         private static final Pool s_pool = new Pool(CallbackStackItem::new);

         public boolean test(Object var1) {
            return this.predicate.test(var1, this.val1);
         }

         public static CallbackStackItem alloc(Object var0, ICallback var1) {
            CallbackStackItem var2 = (CallbackStackItem)s_pool.alloc();
            var2.val1 = var0;
            var2.predicate = var1;
            return var2;
         }

         public void onReleased() {
            this.val1 = null;
            this.predicate = null;
         }
      }

      private static class StackItem extends PooledObject {
         Object val1;
      }

      public interface ICallback {
         boolean test(Object var1, Object var2);
      }
   }
}
