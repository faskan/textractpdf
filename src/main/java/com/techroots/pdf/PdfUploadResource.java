package com.techroots.pdf;

import com.techroots.pdf.service.DocumentUploadService;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/pdf")
public class PdfUploadResource {

    @Inject
    DocumentUploadService documentUploadService;

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@MultipartForm MultipartFormDataInput input) {
        return Response.ok().
                entity(documentUploadService.uploadFile(input)).build();
    }


}
