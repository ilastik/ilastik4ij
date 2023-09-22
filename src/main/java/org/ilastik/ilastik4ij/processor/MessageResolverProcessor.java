/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2023 N/A
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
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
