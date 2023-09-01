package org.ilastik.ilastik4ij.processor;

import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.process.AbstractPreprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Plugin;
import org.scijava.widget.InputHarvester;

/**
 * Resolve optional inputs if plugin has {@code resolve-optional} attribute.
 * <p>
 * This avoids pop-up dialogs when all but optional inputs are already resolved.
 */
@Plugin(type = PreprocessorPlugin.class, priority = InputHarvester.PRIORITY + 1)
public final class ResolveOptionalProcessor extends AbstractPreprocessorPlugin {
    public static final String KEY = "resolve-optional";

    @Override
    public void process(Module module) {
        ModuleInfo info = module.getInfo();
        if (!info.is(KEY)) {
            return;
        }
        for (String name : module.getInputs().keySet()) {
            if (!info.getInput(name).isRequired()) {
                module.resolveInput(name);
            }
        }
    }
}
