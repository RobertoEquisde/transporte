# ReporteFinancieroControllerApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**exportarReporteFinanciero**](#exportarreportefinanciero) | **GET** /api/reportes/financiero | |

# **exportarReporteFinanciero**
> exportarReporteFinanciero()


### Example

```typescript
import {
    ReporteFinancieroControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new ReporteFinancieroControllerApi(configuration);

let mes: string; // (default to undefined)

const { status, data } = await apiInstance.exportarReporteFinanciero(
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

