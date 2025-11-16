# LinkCare Performance & Invitation System Analysis

## Summary
Found 3 CRITICAL issues causing 5-30 second response times and broken deep link integration.

## CRITICAL ISSUES

### 1. No Timeout on API Calls
**File:** MainTabs.kt (Lines 267-275)
**Problem:** `scope.launch { groupvm.createInvitationLink() }` can hang indefinitely
**Fix:** Add `withTimeoutOrNull(10000L)` wrapper - 10 second max

### 2. No Loading Indicator
**File:** MainTabs.kt (Lines 267-275)  
**Problem:** User clicks button, 5-15 second freeze with no feedback
**Fix:** Add `var inviteLinkLoading by remember { mutableStateOf(false) }`

### 3. InvitationPreviewScreen Not Registered
**File:** AppNavGraph.kt (Missing entire composable block)
**Problem:** Route exists (Destinations.kt) but NOT in navigation graph
**Fix:** Add composable() with Route.InvitationPreview.route after line 209

### 4. Deep Links Not Handled
**File:** MainActivity.kt (Missing intent processing)
**Problem:** AndroidManifest.xml has intent-filter but onCreate() doesn't read it
**Fix:** Extract token from intent.data and navigate to InvitationPreview route

### 5. No Caching = Duplicate API Calls
**File:** GroupRepository.kt (Lines 12-14)
**Problem:** Every tab switch = new API call even if data unchanged
**Fix:** Cache results for 60 seconds

## PERFORMANCE BOTTLENECKS

### LaunchedEffect Chain (MainTabs.kt Lines 107-122)
```kotlin
// PROBLEMATIC
LaunchedEffect(Unit) {
    groupvm.loadCare()       // API Call every recomposition
    groupvm.loadHealth()     // API Call every recomposition
}
```
**Fix:** Check if cache exists first
```kotlin
LaunchedEffect(Unit) {
    if (careGroups.value.isEmpty()) groupvm.loadCare()
    if (healthGroups.value.isEmpty()) groupvm.loadHealth()
}
```

### Unmemoized Computations (Lines 96-105)
```kotlin
// PROBLEMATIC - recalculated every recomposition
val currentGroupCare = careGroups.getOrNull(selectedIndexCare)?.groupName ?: "케어"
menuItems = careGroups.map { it.groupName }  // New list every time
```
**Fix:**
```kotlin
val currentGroupCare = remember(selectedIndexCare, careGroups) {
    careGroups.getOrNull(selectedIndexCare)?.groupName ?: "케어"
}
val careGroupNames = remember(careGroups) {
    careGroups.map { it.groupName }
}
```

### CareGroupHome Effect Chain (Lines 88-161)
Multiple LaunchedEffects trigger sequentially causing 3+ recompositions per data load.

## INVITATION LINK CREATION FLOW

```
User clicks "초대 링크 복사"
  → MainTabs.kt createInvitationLink() (NO TIMEOUT, NO LOADING STATE)
  → MyGroupsViewModel.createInvitationLink() 
  → GroupRepository.createInvitation()
  → GroupApi.createInvitation() (NETWORK CALL - 5-15 seconds)
  → InvitationResponse returned
  → Result.onSuccess: clipboardManager.setText() + Toast
```

**Problem:** No feedback during 5-15 second API call.

## DEEP LINK INTEGRATION MISSING

**Route defined:** Destinations.kt line 49 ✓
**Screen created:** InvitationPreview.kt ✓  
**AndroidManifest intent-filter:** Present ✓
**AppNavGraph composable registration:** MISSING ✗
**MainActivity intent handling:** MISSING ✗

Result: Links don't work at all.

## FILES TO MODIFY

1. **MainTabs.kt** - Add timeout, loading state, memoization, fix LaunchedEffect
2. **AppNavGraph.kt** - Register InvitationPreviewScreen composable
3. **MainActivity.kt** - Handle deep link intent
4. **GroupRepository.kt** - Add caching (60 second TTL)
5. **CareGroupHome.kt** - Optimize LaunchedEffect chain

## QUICK FIXES (15 minutes)

1. Wrap API call with timeout:
```kotlin
withTimeoutOrNull(10000L) { groupvm.createInvitationLink(...) }
```

2. Add loading state:
```kotlin
var inviteLinkLoading by remember { mutableStateOf(false) }
if (inviteLinkLoading) LoadingOverlay()
```

3. Register InvitationPreview in AppNavGraph (copy from existing composable patterns)

4. Process intent in MainActivity:
```kotlin
val token = intent.data?.pathSegments?.getOrNull(1)
val startDest = if (token != null) Route.InvitationPreview.route else Route.Login.route
```

## EXPECTED RESULTS

- Invitation creation: 5-30s → 0.5-2s with feedback
- Deep links: Broken → Fully functional
- API calls: ~50% reduction via caching
- Tab switching: 2-5s → <500ms
