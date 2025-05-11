# CobrosControllerApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**crear2**](#crear2) | **POST** /api/cobros | |
|[**listarPorUnidad**](#listarporunidad) | **GET** /api/cobros/unidad/{unidadId} | |

# **crear2**
> object crear2(crearCobroRequest)


### Example

```typescript
import {
    CobrosControllerApi,
    Configuration,
    CrearCobroRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new CobrosControllerApi(configuration);

let crearCobroRequest: CrearCobroRequest; //

const { status, data } = await apiInstance.crear2(
    crearCobroRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **crearCobroRequest** | **CrearCobroRequest**|  | |


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

# **listarPorUnidad**
> Array<CobroDTO> listarPorUnidad()


### Example

```typescript
import {
    CobrosControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new CobrosControllerApi(configuration);

let unidadId: number; // (default to undefined)

const { status, data } = await apiInstance.listarPorUnidad(
    unidadId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **unidadId** | [**number**] |  | defaults to undefined|


### Return type

**Array<CobroDTO>**

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

