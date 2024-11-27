package zombie.text.templating;

import java.util.ArrayList;

public class ReplaceList implements IReplace {
   private final ArrayList replacements;

   public ReplaceList() {
      this.replacements = new ArrayList();
   }

   public ReplaceList(ArrayList var1) {
      this.replacements = var1;
   }

   protected ArrayList getReplacements() {
      return this.replacements;
   }

   public String getString() {
      return this.replacements.size() == 0 ? "!ERROR_EMPTY_LIST!" : (String)this.replacements.get(TemplateText.RandNext(this.replacements.size()));
   }
}
