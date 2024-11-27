package zombie.debug;

public enum LogSeverity {
   Trace,
   Debug,
   General,
   Warning,
   Error;

   // $FF: synthetic method
   private static LogSeverity[] $values() {
      return new LogSeverity[]{Trace, Debug, General, Warning, Error};
   }
}
