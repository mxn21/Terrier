package com.mxn.plugin.terrier;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;


public class TerrierToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        TerrierPanel explorer = new TerrierPanel(project);
        final ContentManager contentManager = toolWindow.getContentManager();
        final Content content = contentManager.getFactory().createContent(explorer, "Terrier", false);
        contentManager.addContent(content);

    }
}
