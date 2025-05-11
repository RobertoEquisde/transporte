# ModeloControllerApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**crear**](#crear) | **POST** /api/modelos | |
|[**eliminar**](#eliminar) | **DELETE** /api/modelos/{id} | |
|[**listar2**](#listar2) | **GET** /api/modelos | |

# **crear**
> object crear(crearModeloRequest)


### Example

```typescript
import {
    ModeloControllerApi,
    Configuration,
    CrearModeloRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new ModeloControllerApi(configuration);

let crearModeloRequest: CrearModeloRequest; //

const { status, data } = await apiInstance.crear(
    crearModeloRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **crearModeloRequest** | **CrearModeloRequest**|  | |


### Return type

**object**

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

# **eliminar**
> object eliminar()


### Example

```typescript
import {
    ModeloControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new ModeloControllerApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.eliminar(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**object**

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

# **listar2**
> Array<ModeloDTO> listar2()


### Example

```typescript
import {
    ModeloControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new ModeloControllerApi(configuration);

const { status, data } = await apiInstance.listar2();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<ModeloDTO>**

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

