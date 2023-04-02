package com.example.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.entities.Producto;
import com.example.services.ProductoService;
import com.example.utilities.FileUploadUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/productos")
public class ProductoController {

    @Autowired // Inyectamos el objeto.
    private ProductoService productoService;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    // El método siguiente va a responder a una peticion (request) del tipo:
    // http://localhost:8080/productos?page=3size=4
    // Es decir, tiene que ser capaz de devolver un listado de productos paginados o no, pero en cualquier caso ordenado por un 
    // criterio  de ordenación. 

    // La petición anterior imploca @RequestParam
    // @PathVariable sería pasarle el id del producto que nos tiene que devolver (/productos/3)

    @GetMapping
    public ResponseEntity<List<Producto>> findAll(@RequestParam(name = "page", required = false) Integer page, 
                                                        @RequestParam(name = "size", required = false) Integer size) {
        
        ResponseEntity<List<Producto>> responseEntity = null;    
        List<Producto> productos = new ArrayList<>();
        Sort sortByNombre = Sort.by("nombre");                                                        
        
        if( page != null && size != null) {
            // Con paginación y ordenamiento
            try {
                Pageable pageable = PageRequest.of(page, size, sortByNombre);

                Page<Producto> productosPaginados = productoService.findAll(pageable);

                productos = productosPaginados.getContent();

                responseEntity = new ResponseEntity<List<Producto>>(productos, HttpStatus.OK);



            } catch (Exception e) {
                responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

        } else {
            // Sin paginación, pero con ordenamiento.

            try {
                productos = productoService.findAll(sortByNombre);
                responseEntity = new ResponseEntity<List<Producto>>(productos, HttpStatus.OK);

            } catch (Exception e) {
                responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

            }
        }


        return responseEntity;



    }

    /** 
     * Recupera un producto por el id.
     * Va a responde a una peticion del tipoc, por ejemplo: 
     * http://localhost:8080/productos/2 
     *
     */

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable(name = "id") Integer id) {

        ResponseEntity<Map<String, Object>> responseEntity = null;

        Map<String, Object> responseAsMap = new HashMap<>();




        try {

            Producto producto = productoService.findById(id);

            if (producto != null) {
            String successMessage = "Se ha encontrado el producto con id: " + id + " correctamente";
            responseAsMap.put("mensaje", successMessage);
            responseAsMap.put("producto", producto);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.OK);
            
        } else {

            String errorMessage = "No se ha encontrado el producto con id: " + id;
            responseAsMap.put("error", errorMessage);
            responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.NOT_FOUND);

        }

        } catch (Exception e) {

            String errorGrave = "Error grave";
            responseAsMap.put("error", errorGrave);
            responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);

          
        }



        return responseEntity;
    }


    /**
     * Persiste un producto en la base de datos:
     * @throws IOException
     */

    // Guardar (Persistir), un producto, con su presentacion en la base de datos
    // Para probarlo con POSTMAN: Body -> form-data -> producto -> CONTENT TYPE ->
    // application/json
    // no se puede dejar el content type en Auto, porque de lo contrario asume
    // application/octet-stream
    // y genera una exception MediaTypeNotSupported

    @PostMapping( consumes = "multipart/form-data")
    @Transactional
    public ResponseEntity<Map<String, Object>> insert(
                            @Valid @RequestBody Producto producto, 
                            BindingResult result,
                            @RequestParam (name = "file") MultipartFile file ) throws IOException {

        Map<String, Object> responseAsMap = new HashMap<>();

        ResponseEntity<Map<String, Object>> responseEntity = null;

        /** 
         * Primero debemos comprobar si hay errores en el producto recibido. 
         */

         if (result.hasErrors()) {

            List<String> errorMessages = new ArrayList<>();

            // for( ObjectError error : result.getAllErrors()) {

            //     errorMessages.add(error.getDefaultMessage());

            // }

            var prueba = result.getAllErrors();

            prueba.stream().forEach(e -> {
                errorMessages.add(e.getDefaultMessage());
            });


            responseAsMap.put("errores", errorMessages);

            responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.BAD_REQUEST);

            return responseEntity;

        




         }

         // Si no hay errores, entonces persistimos el producto. Comprobando previamente si nos han enviado una imagen o un archivo. 

         if(!file.isEmpty()) {
            String fileCode =  fileUploadUtil.saveFile(file.getOriginalFilename(), file);
            producto.setImagenProducto(fileCode+"-"+file.getOriginalFilename());
         }

         Producto productoDB = productoService.save(producto);

         try {

            if (productoDB != null) {

                String mensaje = "El producto se ha creado correctamente" ;
                responseAsMap.put("mensaje", mensaje);
                responseAsMap.put("producto", productoDB);
                responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.CREATED);
    
             } else {

                String mensaje = "El producto no se ha creado correctamente" ;

    
                responseAsMap.put("mensaje", mensaje);

                responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.BAD_REQUEST );
    
             }
            
         } catch (DataAccessException e) {

            String errorGrave = "Ha tenido lugar un error grave  y, la causa más problable puede ser: " + e.getMostSpecificCause();

           responseAsMap.put("errorGrave", errorGrave);

           responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
        
        }



        return responseEntity;
    }


    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Producto producto, BindingResult result, @PathVariable(name = "id") Integer id) {

        Map<String, Object> responseAsMap = new HashMap<>();

        ResponseEntity<Map<String, Object>> responseEntity = null;

        /** 
         * Primero debemos comprobar si hay errores en el producto recibido. 
         */

         if (result.hasErrors()) {

            List<String> errorMessages = new ArrayList<>();

            // for( ObjectError error : result.getAllErrors()) {

            //     errorMessages.add(error.getDefaultMessage());

            // }

            var prueba = result.getAllErrors();

            prueba.stream().forEach(e -> {
                errorMessages.add(e.getDefaultMessage());
            });


            responseAsMap.put("errores", errorMessages);

            responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.BAD_REQUEST);

            return responseEntity;

        




         }

         // Si no hay errores, entonces persistimos el producto. 

         producto.setId(id);
         Producto productoDB = productoService.save(producto);

         try {

            if (productoDB != null) {

                String mensaje = "El producto se ha actualizado correctamente" ;
                responseAsMap.put("mensaje", mensaje);
                responseAsMap.put("producto", productoDB);
                responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.CREATED);
    
             } else {

                String mensaje = "El producto no se ha actualizado correctamente" ;

    
                responseAsMap.put("mensaje", mensaje);

                responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.BAD_REQUEST );
    
             }
            
         } catch (DataAccessException e) {

            String errorGrave = "Ha tenido lugar un error grave  y, la causa más problable puede ser: " + e.getMostSpecificCause();

           responseAsMap.put("errorGrave", errorGrave);

           responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
        
        }



        return responseEntity;
    }
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<String> delete(@Valid @RequestBody Producto producto, @PathVariable(name = "id") Integer id) {


        ResponseEntity<String> responseEntity = null;
        try {
           

            Producto productoDelete = productoService.findById(id);

            


            if (productoDelete != null) {

                productoService.delete(productoDelete);

                String mensajeOk = "Se ha borrado correctamente.";

                responseEntity = new ResponseEntity<String>(mensajeOk, HttpStatus.OK);

                              



            } else {

                String mensajeError = "No existe el producto que quiere borrar.";

                responseEntity = new ResponseEntity<String>(mensajeError, HttpStatus.NOT_FOUND);
            }

    
            
           } catch (DataAccessException e) {
            e.getMostSpecificCause();
              responseEntity = new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
            
             }


       
    return responseEntity;



        
    }








    /** El método siguiente es un EJEMPLO para entender el formato JSON. No tiene que ver con el proyecto  */
    
    // @GetMapping
    // public List<String> nombres() {

    //     List<String> nombres = Arrays.asList("Salma", "Judith", "Elisabet");

    //     return nombres;
    // }
}
