# SeguroControllerApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**buscarPorFactura**](#buscarporfactura) | **GET** /api/seguros/buscar | |
|[**crearSeguro**](#crearseguro) | **POST** /api/seguros | |
|[**eliminarPorFactura**](#eliminarporfactura) | **DELETE** /api/seguros/factura/{factura} | |
|[**listar1**](#listar1) | **GET** /api/seguros | |

# **buscarPorFactura**
> Array<SeguroDTO> buscarPorFactura()


### Example

```typescript
import {
    SeguroControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SeguroControllerApi(configuration);

let factura: string; // (default to undefined)

const { status, data } = await apiInstance.buscarPorFactura(
    factura
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **factura** | [**string**] |  | defaults to undefined|


### Return type

**Array<SeguroDTO>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **crearSeguro**
> SeguroDTO crearSeguro(crearSeguroRequest)


### Example

```typescript
import {
    SeguroControllerApi,
    Configuration,
    CrearSeguroRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new SeguroControllerApi(configuration);

let crearSeguroRequest: CrearSeguroRequest; //

const { status, data } = await apiInstance.crearSeguro(
    crearSeguroRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **crearSeguroRequest** | **CrearSeguroRequest**|  | |


### Return type

**SeguroDTO**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **eliminarPorFactura**
> string eliminarPorFactura()


### Example

```typescript
import {
    SeguroControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SeguroControllerApi(configuration);

let factura: string; // (default to undefined)

const { status, data } = await apiInstance.eliminarPorFactura(
    factura
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **factura** | [**string**] |  | defaults to undefined|


### Return type

**string**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listar1**
> Array<SeguroDTO> listar1()


### Example

```typescript
import {
    SeguroControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SeguroControllerApi(configuration);

const { status, data } = await apiInstance.listar1();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<SeguroDTO>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

