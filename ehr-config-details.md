Follow your EHR vendor's instructions to register and integrate the CDS Hooks service.

The following may be helpful in configuring the service in Epic and other EHR systems.

# Details for presumptive-gc-treatment service
* The CDS Hook request should be configured to work on "order-sign".
* Restrict the CDS service to trigger under the following conditions:
  * During ED and urgent care visits
  * When the order is being written for Ceftriaxone IM

# Details for confirmed-gc-treatment service
* The CDS Hook request should be configured to work on "patient-view".
* Restrict the CDS service to trigger under the following conditions:
  * During ED and urgent care visits
  * Patients who have a result for gonorrhea culture test

# Create a preference list:
The card suggestions include standard terminology codes for the associated orders. In addition, in Epic a preference list can be created that allows the orders to be created from the best practice advisory dialog. Include the following items in the preference list:
1. Order for Ceftriaxone 500 mg IM without lidocaine (item 1)
2. Order for Ceftriaxone 500 mg IM with lidocaine (item 2)
3. Order for Gentamicin 240 mg IM (item 3)
4. Order for Azithromycin 2 g PO (item 4)
5. Order for Ceftriaxone 1000 mg IM with lidocaine (item 5)
6. Order for Ceftriaxone 1000 mg IM without lidocaine (item 6)
7. Order for HIV antigen test (item 7)
8. Order for referral to infectious disease (item 20)

# Override reasons for presumptive-gc-treatment service
Map the following override reason codes to acknowledge reasons in the other EHR:

|Override Reason Code| Override Reason Description|
|--------------------|----------------------------|
|notforgonorrhea|Treatment not for gonorrhea|
|toleratesctx|Patient tolerates ceftriaxone|
|other|Other|
|alreadytreated|Patient already treated|
|notprimaryteam|I am not the primary team|
|prepinfoprovided|Info about PrEP provided|
|notappropriate|Not appropriate at this time|
|otherprep|Other|
