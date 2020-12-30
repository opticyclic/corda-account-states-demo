# Corda Account States Demo

Demonstrate how to add properties to Corda Account states.

This is a slightly modified version of the Corda Samples for transferring IOUs, except that this uses accounts.
The demo is designed to run with all the accounts on a single node to simplify the flows.

A new type of Account is created (called _Corp_) with a property that determines the company type (BANK or AGENT in this case).

The flow to create the custom account (`CreateCorp`) first creates a Corda Account, then creates a new `Corp` state with the newly created Corda account as a property.

You will see that you can create the IOUs with both the "normal" SDK accounts and also the new custom accounts.
The project doesn't demonstrate accessing the company type, although it is fairly simple to do as long as you pass a `Corp` type to your function and not an `AccountInfo` type. 

## Debugging

Check that the `tools.jar` has been added to your project JDK classpath and that Gradle is running with a JDK and not a JRE or any test mocks won't work properly.

## Local Testing

The accounts SDK _also_ has a custom schema, so `runSchemaMigration = true` is added to the `deployNodes` gradle task in order to create those tables.

For development, we can let Hibernate create the tables and manage the schema.

Start the nodes with:

    cd build/nodes/Notary;java -jar corda.jar
    cd build/nodes/Banks;java -jar corda.jar --allow-hibernate-to-manage-app-schema

For production, you would need to have Liquibase scripts but this project is just a demo.

## Run Commands From The CRaSH Shell

**List all the flows registered on the node**

    flow list

**Create Accounts**

These accounts are the standard Corda Accounts SDK version.

    flow start CreateAccount name: 'Bank1'
    flow start CreateAccount name: 'Agent1'

**Create Custom Accounts**

These accounts will have an extra property denoting the "type" of account.

_If you have already created the standard accounts you will have to use different names to the ones below as they will clash._

    flow start CreateCorp companyName: 'Bank1', companyType: 'BANK'
    flow start CreateCorp companyName: 'Agent1', companyType: 'AGENT'

**List Accounts on the node**

    flow start AllAccounts

**Send an IOU from Agent1 to Bank1**

    flow start IOUAccountFlow iouValue: 50, lender: Bank1, borrower: Agent1

**Get all IOU states**

    run vaultQuery contractStateType: com.github.opticyclic.corda.demo.accounts.states.IOUAccountState

**Get all states**

    run vaultQuery contractStateType: net.corda.core.contracts.ContractState
