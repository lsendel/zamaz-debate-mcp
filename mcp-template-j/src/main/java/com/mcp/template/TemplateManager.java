package com.mcp.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TemplateManager {

    private final TemplateRepository templateRepository;

    @Autowired
    public TemplateManager(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public Template createTemplate(Template template) {
        if (template.getId() == null) {
            template.setId(UUID.randomUUID().toString());
        }
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        return templateRepository.save(template);
    }

    public Optional<Template> getTemplate(String templateId) {
        return templateRepository.findById(templateId);
    }

    public List<Template> getTemplatesByOrganization(String organizationId) {
        return templateRepository.findByOrganizationId(organizationId);
    }

    public Template updateTemplate(String templateId, Template updatedTemplate) {
        return templateRepository.findById(templateId).map(template -> {
            template.setName(updatedTemplate.getName());
            template.setDescription(updatedTemplate.getDescription());
            template.setCategory(updatedTemplate.getCategory());
            template.setSubcategory(updatedTemplate.getSubcategory());
            template.setContent(updatedTemplate.getContent());
            template.setVariables(updatedTemplate.getVariables());
            template.setTags(updatedTemplate.getTags());
            template.setStatus(updatedTemplate.getStatus());
            template.setMetadata(updatedTemplate.getMetadata());
            template.setVersion(template.getVersion() + 1);
            template.setUpdatedAt(LocalDateTime.now());
            return templateRepository.save(template);
        }).orElseThrow(() -> new RuntimeException("Template not found"));
    }

    public void deleteTemplate(String templateId) {
        templateRepository.deleteById(templateId);
    }
}
