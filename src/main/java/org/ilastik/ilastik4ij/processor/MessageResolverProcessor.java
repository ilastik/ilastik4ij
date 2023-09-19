package org.ilastik.ilastik4ij.processor;

import org.scijava.ItemVisibility;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.process.AbstractPreprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Plugin;
import org.scijava.widget.InputHarvester;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolve all inputs with {@link ItemVisibility#MESSAGE} if all other inputs have been resolved.
 * <p>
 * This is necessary for macros and batch processing where users don't interact with the GUI.
 */
@Plugin(type = PreprocessorPlugin.class, priority = InputHarvester.PRIORITY + 1)
public final class MessageResolverProcessor extends AbstractPreprocessorPlugin {
    @Override
    public void process(Module module) {
        // Adapted from https://github.com/scijava/scijava-common/issues/299#issuecomment-1258186868

        ModuleInfo info = module.getInfo();
        List<String> messageNames = new ArrayList<>();

        for (String name : module.getInputs().keySet()) {
            if (info.getInput(name).getVisibility() == ItemVisibility.MESSAGE) {
                messageNames.add(name);
            } else if (!module.isInputResolved(name)) {
                // If at least one non-message input hasn't been resolved yet,
                // don't touch message inputs.
                // Otherwise, these messages won't show up in the GUI.
                return;
            }
        }

        messageNames.forEach(module::resolveInput);
    }
}