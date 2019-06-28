/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.studio.web;

import com.axelor.apps.base.db.App;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.studio.db.ModuleBuilder;
import com.axelor.studio.db.repo.ModuleBuilderRepository;
import com.axelor.studio.exception.IExceptionMessage;
import com.axelor.studio.service.excel.exporter.DataWriter;
import com.axelor.studio.service.excel.exporter.ExcelExporterService;
import com.axelor.studio.service.excel.exporter.ExcelWriter;
import com.axelor.studio.service.excel.importer.DataReaderService;
import com.axelor.studio.service.excel.importer.ExcelImporterService;
import com.axelor.studio.service.excel.importer.ExcelReaderService;
import com.axelor.studio.service.module.ModuleExportService;
import com.axelor.studio.service.module.ModuleImportService;
import com.axelor.studio.service.module.ModuleInstallService;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ModuleBuilderController {

  private static final String buildLogFile = "AppBuildLog.txt";

  @Inject private ModuleExportService moduleExportService;

  @Inject private ModuleImportService moduleImportService;

  @Inject private ModuleInstallService moduleInstallService;

  @Inject private ExcelExporterService excelExporterService;

  @Inject private ExcelImporterService excelImporterService;

  @Inject private ModuleBuilderRepository moduleBuilderRepo;

  public void exportModule(ActionRequest request, ActionResponse response) {
    try {
      ModuleBuilder moduleBuilder = request.getContext().asType(ModuleBuilder.class);
      moduleBuilder = moduleBuilderRepo.find(moduleBuilder.getId());
      MetaFile exportFile = null;

      if (moduleBuilder.getFileTypeSelect().equals(ModuleBuilderRepository.FILE_TYPE_ZIP)) {

        exportFile =
            moduleExportService.export(moduleBuilder.getName(), moduleBuilder.getImportMetaFile());

      } else if (moduleBuilder
          .getFileTypeSelect()
          .equals(ModuleBuilderRepository.FILE_TYPE_EXCEL)) {

        DataWriter writer = new ExcelWriter();
        DataReaderService reader = new ExcelReaderService();

        exportFile = excelExporterService.export(moduleBuilder.getName(), writer, reader);
      }
      response.setValue("exportMetaFile", exportFile);
    } catch (Exception e) {
      response.setError(e.getMessage());
      TraceBackService.trace(e);
    }
  }

  public void importModule(ActionRequest request, ActionResponse response) {
    try {
      ModuleBuilder moduleBuilder = request.getContext().asType(ModuleBuilder.class);
      moduleBuilder = moduleBuilderRepo.find(moduleBuilder.getId());

      if (moduleBuilder.getFileTypeSelect().equals(ModuleBuilderRepository.FILE_TYPE_ZIP)) {

        moduleImportService.importModule(moduleBuilder);
        response.setFlash(I18n.get(IExceptionMessage.MODULE_IMPORTED));

      } else if (moduleBuilder
          .getFileTypeSelect()
          .equals(ModuleBuilderRepository.FILE_TYPE_EXCEL)) {

        DataReaderService reader = new ExcelReaderService();
        MetaFile importFile = moduleBuilder.getImportMetaFile();

        MetaFile file =
            excelImporterService.importExcel(moduleBuilder.getName(), reader, importFile);

        if (!file.getFileName().startsWith("Log")) {
          response.setValue("generatedFile", file);
          response.setFlash(I18n.get(IExceptionMessage.MODULE_IMPORTED));
        } else {
          response.setView(
              ActionView.define(I18n.get("Import error log"))
                  .model(App.class.getName())
                  .add(
                      "html",
                      "ws/rest/com.axelor.meta.db.MetaFile/"
                          + file.getId()
                          + "/content/download?v="
                          + file.getVersion())
                  .param("download", "true")
                  .map());
        }
      }
    } catch (Exception e) {
      response.setError(e.getMessage());
      TraceBackService.trace(e);
    }
  }

  public void restartServer(ActionRequest request, ActionResponse response) {

    try {
      String errorLog = moduleInstallService.buildApp();
      if (errorLog != null) {
        response.setFlash(I18n.get(IExceptionMessage.BUILD_LOG_CHECK));
        downloadLogFile(response, errorLog);
      } else {
        moduleInstallService.restartServer(false);
      }

    } catch (Exception e) {
      response.setError(e.getMessage());
      TraceBackService.trace(e);
    }
  }

  private void downloadLogFile(ActionResponse response, String errorLog) throws IOException {

    MetaFiles metaFiles = Beans.get(MetaFiles.class);
    MetaFile metaFile =
        metaFiles.upload(new ByteArrayInputStream(errorLog.getBytes()), buildLogFile);

    response.setView(
        ActionView.define(I18n.get("Build error log"))
            .model(App.class.getName())
            .add(
                "html",
                "ws/rest/com.axelor.meta.db.MetaFile/"
                    + metaFile.getId()
                    + "/content/download?v="
                    + metaFile.getVersion())
            .param("download", "true")
            .map());
  }
}