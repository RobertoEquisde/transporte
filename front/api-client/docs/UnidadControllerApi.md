# UnidadControllerApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**buscarPorUltimosDigitos**](#buscarporultimosdigitos) | **GET** /api/unidades/buscar-por-digitos | |
|[**crearUnidad**](#crearunidad) | **POST** /api/unidades | |
|[**eliminarUnidadPorSerie**](#eliminarunidadporserie) | **DELETE** /api/unidades/eliminar | |
|[**exportExcelPorMesDistribuidora**](#exportexcelpormesdistribuidora) | **GET** /api/unidades/reportes/ | |
|[**listar**](#listar) | **GET** /api/unidades | |
|[**obtenerPorNumeroSerie**](#obtenerpornumeroserie) | **GET** /api/unidades/buscar | |

# **buscarPorUltimosDigitos**
> Array<UnidadDTO> buscarPorUltimosDigitos()


### Example

```typescript
import {
    UnidadControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new UnidadControllerApi(configuration);

let terminaEn: string; // (default to undefined)

const { status, data } = await apiInstance.buscarPorUltimosDigitos(
    terminaEn
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **terminaEn** | [**string**] |  | defaults to undefined|


### Return type

**Array<UnidadDTO>**

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

# **crearUnidad**
> UnidadDTO crearUnidad(crearUnidadRequest)


### Example

```typescript
import {
    UnidadControllerApi,
    Configuration,
    CrearUnidadRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new UnidadControllerApi(configuration);

let crearUnidadRequest: CrearUnidadRequest; //

const { status, data } = await apiInstance.crearUnidad(
    crearUnidadRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **crearUnidadRequest** | **CrearUnidadRequest**|  | |


### Return type

**UnidadDTO**

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

# **eliminarUnidadPorSerie**
> string eliminarUnidadPorSerie()


### Example

```typescript
import {
    UnidadControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new UnidadControllerApi(configuration);

let serie: string; // (default to undefined)

const { status, data } = await apiInstance.eliminarUnidadPorSerie(
    serie
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **serie** | [**string**] |  | defaults to undefined|


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

# **exportExcelPorMesDistribuidora**
> exportExcelPorMesDistribuidora()


### Example

```typescript
import {
    UnidadControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new UnidadControllerApi(configuration);

let mes: string; // (default to undefined)

const { status, data } = await apiInstance.exportExcelPorMesDistribuidora(
    mes
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **mes** | [**string**] |  | defaults to undefined|


### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listar**
> Array<UnidadDTO> listar()


### Example

```typescript
import {
    UnidadControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new UnidadControllerApi(configuration);

const { status, data } = await apiInstance.listar();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<UnidadDTO>**

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

# **obtenerPorNumeroSerie**
> UnidadDTO obtenerPorNumeroSerie()


### Example

```typescript
import {
    UnidadControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new UnidadControllerApi(configuration);

let serie: string; // (default to undefined)

const { status, data } = await apiInstance.obtenerPorNumeroSerie(
    serie
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **serie** | [**string**] |  | defaults to undefined|


### Return type

**UnidadDTO**

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

