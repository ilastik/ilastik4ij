package org.ilastik.ilastik4ij.processor;

import org.scijava.ItemVisibility;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.process.AbstractPreprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Plugin;
import org.scijava.widget.InputHarvester;

/**
 * Scijava processor that resolves all inputs with MESSAGE {@link ItemVisibility},
 * if they are the only ones remaining, before the InputHarvester kicks in.
 */
@Plugin(type = PreprocessorPlugin.class, priority = InputHarvester.PRIORITY + 1)
public final class MessageResolverProcessor extends AbstractPreprocessorPlugin {
    @Override
    public void process(Module module) {
        // Adapted from https://github.com/scijava/scijava-common/issues/317#issuecomment-1128132787
        ModuleInfo info = module.getInfo();
        for (String name : module.getInputs().keySet()) {
            if (info.getInput(name).getVisibility() == ItemVisibility.MESSAGE) {
                module.resolveInput(name);
            }
        }
    }
}