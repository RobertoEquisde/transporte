# DistribuidorControllerApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**buscarPorClave**](#buscarporclave) | **GET** /api/distribuidores/buscar | |
|[**crear1**](#crear1) | **POST** /api/distribuidores | |
|[**eliminar1**](#eliminar1) | **DELETE** /api/distribuidores/{id} | |
|[**listar3**](#listar3) | **GET** /api/distribuidores | |

# **buscarPorClave**
> Array<DistribuidoraInfoDTO> buscarPorClave()


### Example

```typescript
import {
    DistribuidorControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new DistribuidorControllerApi(configuration);

let clave: string; // (default to undefined)

const { status, data } = await apiInstance.buscarPorClave(
    clave
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **clave** | [**string**] |  | defaults to undefined|


### Return type

**Array<DistribuidoraInfoDTO>**

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

# **crear1**
> Distribuidor crear1(crearDistribuidorDTO)


### Example

```typescript
import {
    DistribuidorControllerApi,
    Configuration,
    CrearDistribuidorDTO
} from './api';

const configuration = new Configuration();
const apiInstance = new DistribuidorControllerApi(configuration);

let crearDistribuidorDTO: CrearDistribuidorDTO; //

const { status, data } = await apiInstance.crear1(
    crearDistribuidorDTO
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **crearDistribuidorDTO** | **CrearDistribuidorDTO**|  | |


### Return type

**Distribuidor**

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

# **eliminar1**
> object eliminar1()


### Example

```typescript
import {
    DistribuidorControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new DistribuidorControllerApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.eliminar1(
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

# **listar3**
> Array<DistribuidoraInfoDTO> listar3()


### Example

```typescript
import {
    DistribuidorControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new DistribuidorControllerApi(configuration);

const { status, data } = await apiInstance.listar3();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<DistribuidoraInfoDTO>**

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

