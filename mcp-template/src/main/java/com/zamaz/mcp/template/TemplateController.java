package com.zamaz.mcp.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/templates")
public class TemplateController {

    private final TemplateManager templateManager;

    @Autowired
    public TemplateController(TemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    @PostMapping
    public Template createTemplate(@RequestBody Template template) {
        return templateManager.createTemplate(template);
    }

    @GetMapping("/{templateId}")
    public Template getTemplate(@PathVariable String templateId) {
        return templateManager.getTemplate(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    @GetMapping("/organization/{organizationId}")
    public List<Template> getTemplatesByOrganization(@PathVariable String organizationId) {
        return templateManager.getTemplatesByOrganization(organizationId);
    }

    @PutMapping("/{templateId}")
    public Template updateTemplate(@PathVariable String templateId, @RequestBody Template template) {
        return templateManager.updateTemplate(templateId, template);
    }

    @DeleteMapping("/{templateId}")
    public void deleteTemplate(@PathVariable String templateId) {
        templateManager.deleteTemplate(templateId);
    }
}
